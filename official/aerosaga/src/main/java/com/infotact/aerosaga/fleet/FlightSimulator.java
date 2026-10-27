package com.infotact.aerosaga.fleet;

import java.util.ArrayList;
import java.util.List;

import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.MissionException;
import com.infotact.aerosaga.domain.MissionException.Code;
import com.infotact.aerosaga.domain.NoFlyZone;
import com.infotact.aerosaga.domain.OperatingLimits;
import com.infotact.aerosaga.domain.RoutePlan;

/**
 * Simulates the drone flying one leg at a time, with safety checks a real autopilot would make.
 *
 * Safety rules, checked in this order:
 *  1. Telemetry link must be up (FLAKY_TELEMETRY fails early attempts; Temporal retries).
 *  2. The leg must not enter or cross any no-fly zone, permanent or temporary.
 *  3. After the leg, battery must still cover a direct flight home plus the reserve.
 *
 * Failure checks 1 and 2 happen BEFORE the drone moves, so a failed attempt changes nothing.
 */
public class FlightSimulator {

    /** Leg on which injected faults fire. */
    static final int FAULT_LEG = 0;
    /** Extra battery lost when the LOW_BATTERY cell fault fires. */
    static final double CELL_FAULT_DRAIN_PCT = 70.0;
    /** Telemetry attempts that fail before the link recovers under FLAKY_TELEMETRY. */
    static final int FLAKY_FAILED_ATTEMPTS = 2;
    static final double INJECTED_ZONE_RADIUS_KM = 0.5;

    private final DroneFleet fleet;
    private final AirspaceRegistry airspace;
    private final OperatingLimits limits;

    public FlightSimulator(DroneFleet fleet, AirspaceRegistry airspace, OperatingLimits limits) {
        this.fleet = fleet;
        this.airspace = airspace;
        this.limits = limits;
    }

    /**
     * Fly one outbound leg.
     *
     * @param attempt 1 for the first try, 2 for the first retry, and so on
     */
    public LegTelemetry flyLeg(String droneId, String missionId, RoutePlan.Leg leg, double payloadKg,
                               FailureScenario scenario, int attempt) {
        if (scenario == FailureScenario.FLAKY_TELEMETRY && leg.index() == FAULT_LEG && attempt <= FLAKY_FAILED_ATTEMPTS) {
            throw new MissionException(Code.TELEMETRY_LOST,
                    "Telemetry link lost on leg " + leg.index() + " (attempt " + attempt + ")");
        }
        if (scenario == FailureScenario.GEOFENCE_BREACH && leg.index() == FAULT_LEG) {
            airspace.addTemporaryZone(new NoFlyZone("TFR-" + missionId, leg.to(), INJECTED_ZONE_RADIUS_KM));
        }
        for (NoFlyZone zone : activeZones()) {
            if (zone.contains(leg.to()) || zone.crossedBy(leg.from(), leg.to())) {
                throw new MissionException(Code.GEOFENCE_BREACH,
                        "Leg " + leg.fromName() + " -> " + leg.toName() + " would enter no-fly zone '" + zone.name() + "'");
            }
        }

        DroneSpec spec = fleet.spec(droneId);
        fleet.setStatus(droneId, missionId, DroneStatus.IN_FLIGHT);
        double used = BatteryModel.percentFor(spec, payloadKg, leg.distanceKm());
        if (scenario == FailureScenario.LOW_BATTERY && leg.index() == FAULT_LEG) {
            used += CELL_FAULT_DRAIN_PCT;
        }
        double remaining = fleet.recordFlight(droneId, missionId, leg.to(), used);

        double homeCost = BatteryModel.percentFor(spec, payloadKg, leg.to().distanceKmTo(fleet.home()));
        if (remaining < homeCost + limits.batteryReservePct()) {
            throw new MissionException(Code.LOW_BATTERY, String.format(
                    "Battery %.1f%% at %s cannot cover return (%.1f%%) plus reserve (%.0f%%)",
                    remaining, leg.toName(), homeCost, limits.batteryReservePct()));
        }
        return new LegTelemetry(leg.index(), leg.toName(), leg.to(), leg.distanceKm(), round1(remaining),
                seconds(leg.distanceKm(), spec));
    }

    /**
     * Fly straight home from wherever the drone is. Used for the normal return and for emergency aborts.
     * Never refuses: bringing the drone home is always the safest action.
     */
    public LegTelemetry returnToHome(String droneId, String missionId, double payloadKg) {
        DroneSpec spec = fleet.spec(droneId);
        GeoPoint from = fleet.find(droneId).orElseThrow().position();
        double km = from.distanceKmTo(fleet.home());
        fleet.setStatus(droneId, missionId, DroneStatus.RETURNING);
        double remaining = fleet.recordFlight(droneId, missionId, fleet.home(), BatteryModel.percentFor(spec, payloadKg, km));
        fleet.setStatus(droneId, missionId, DroneStatus.ASSIGNED);
        return new LegTelemetry(-1, RoutePlan.BASE_NAME, fleet.home(), km, round1(remaining), seconds(km, spec));
    }

    private List<NoFlyZone> activeZones() {
        List<NoFlyZone> all = new ArrayList<>(limits.noFlyZones());
        all.addAll(airspace.temporaryZones());
        return all;
    }

    private static long seconds(double km, DroneSpec spec) {
        return Math.round(km / spec.cruiseSpeedKmh() * 3600);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
