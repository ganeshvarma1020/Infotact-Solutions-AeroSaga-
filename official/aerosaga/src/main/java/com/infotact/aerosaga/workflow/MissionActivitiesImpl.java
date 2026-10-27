package com.infotact.aerosaga.workflow;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.MissionException;
import com.infotact.aerosaga.domain.MissionException.Code;
import com.infotact.aerosaga.domain.MissionPlanValidator;
import com.infotact.aerosaga.domain.MissionRequest;
import com.infotact.aerosaga.domain.OperatingLimits;
import com.infotact.aerosaga.domain.RoutePlan;
import com.infotact.aerosaga.domain.ValidationResult;
import com.infotact.aerosaga.fleet.DroneFleet;
import com.infotact.aerosaga.fleet.DroneSnapshot;
import com.infotact.aerosaga.fleet.FlightSimulator;
import com.infotact.aerosaga.fleet.LegTelemetry;
import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;

/**
 * Connects workflow steps to the simulator.
 *
 * Error rule: business failures (low battery, no drone, ...) become NON-retryable Temporal failures,
 * because trying again will not help. TELEMETRY_LOST is retryable: the link may come back.
 * Any other unexpected exception is retried by Temporal automatically.
 */
public class MissionActivitiesImpl implements MissionActivities {

    private static final Logger log = LoggerFactory.getLogger(MissionActivitiesImpl.class);

    private final MissionPlanValidator validator;
    private final OperatingLimits limits;
    private final DroneFleet fleet;
    private final AirspaceRegistry airspace;
    private final FlightSimulator simulator;
    private final MissionJournal journal;
    private final Runnable checkpoint;

    /** Without state saving (tests). */
    public MissionActivitiesImpl(MissionPlanValidator validator, OperatingLimits limits, DroneFleet fleet,
                                 AirspaceRegistry airspace, FlightSimulator simulator, MissionJournal journal) {
        this(validator, limits, fleet, airspace, simulator, journal, () -> { });
    }

    /**
     * @param checkpoint saves the simulated world; runs after every step that changes it,
     *                   even if the step failed half-way (a drone can move and then report low battery)
     */
    public MissionActivitiesImpl(MissionPlanValidator validator, OperatingLimits limits, DroneFleet fleet,
                                 AirspaceRegistry airspace, FlightSimulator simulator, MissionJournal journal,
                                 Runnable checkpoint) {
        this.validator = validator;
        this.limits = limits;
        this.fleet = fleet;
        this.airspace = airspace;
        this.simulator = simulator;
        this.journal = journal;
        this.checkpoint = checkpoint;
    }

    @Override
    public RoutePlan planRoute(MissionRequest request) {
        ValidationResult result = validator.validate(request);
        if (!result.valid()) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Mission plan rejected: " + String.join("; ", result.errors()), Code.INVALID_MISSION.name());
        }
        return request.route();
    }

    @Override
    public String reserveAirspace(String missionId) {
        return saving(() -> guarded(() -> airspace.reserve(missionId)));
    }

    @Override
    public void releaseAirspace(String missionId) {
        saving(() -> airspace.release(missionId));
        log.info("[{}] airspace slot released", missionId);
    }

    @Override
    public DroneSnapshot assignDrone(String missionId, double payloadKg, double routeKm) {
        return saving(() -> guarded(() -> fleet.assign(missionId, payloadKg, routeKm, limits.batteryReservePct())));
    }

    @Override
    public void releaseDrone(String missionId) {
        saving(() -> fleet.releaseByMission(missionId));
        log.info("[{}] drone released", missionId);
    }

    @Override
    public void loadPayload(String missionId, double payloadKg) {
        saving(() -> fleet.loadPayload(droneOf(missionId), missionId, payloadKg));
    }

    @Override
    public void unloadPayload(String missionId) {
        saving(() -> fleet.droneFor(missionId).ifPresent(d -> fleet.unloadPayload(d, missionId)));
    }

    @Override
    public LegTelemetry flyLeg(String missionId, RoutePlan.Leg leg, double payloadKg, FailureScenario scenario) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        if (attempt > 1) {
            log.info("[{}] leg {} retry attempt {}", missionId, leg.index(), attempt);
        }
        return saving(() -> guarded(() -> simulator.flyLeg(droneOf(missionId), missionId, leg, payloadKg, scenario, attempt)));
    }

    @Override
    public void deliverPayload(String missionId) {
        saving(() -> fleet.unloadPayload(droneOf(missionId), missionId));
    }

    @Override
    public LegTelemetry returnToHome(String missionId) {
        return saving(() -> fleet.droneFor(missionId)
                .map(d -> simulator.returnToHome(d, missionId, fleet.find(d).orElseThrow().payloadKg()))
                // No drone was ever assigned: nothing to fly home. Report "at base".
                .orElseGet(() -> new LegTelemetry(-1, RoutePlan.BASE_NAME, fleet.home(), 0, 0, 0)));
    }

    @Override
    public void recordEvent(MissionEvent event) {
        journal.event(event);
    }

    @Override
    public void recordOutcome(MissionResult result) {
        journal.outcome(result);
    }

    private String droneOf(String missionId) {
        return fleet.droneFor(missionId).orElseThrow(() -> ApplicationFailure.newNonRetryableFailure(
                "No drone assigned to " + missionId, Code.NO_DRONE_AVAILABLE.name()));
    }

    /** Run a step that changes the world, then save the world, whether the step succeeded or not. */
    private <T> T saving(Supplier<T> action) {
        try {
            return action.get();
        } finally {
            checkpoint.run();
        }
    }

    private void saving(Runnable action) {
        saving(() -> {
            action.run();
            return null;
        });
    }

    /** Run a simulator call and translate business failures into Temporal failures. */
    private static <T> T guarded(Supplier<T> action) {
        try {
            return action.get();
        } catch (MissionException e) {
            if (e.code() == Code.TELEMETRY_LOST) {
                throw ApplicationFailure.newFailure(e.getMessage(), e.code().name());
            }
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), e.code().name());
        }
    }
}
