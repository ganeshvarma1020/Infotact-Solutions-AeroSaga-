package com.infotact.aerosaga.workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.MissionRequest;
import com.infotact.aerosaga.domain.RoutePlan;
import com.infotact.aerosaga.domain.Waypoint;
import com.infotact.aerosaga.fleet.DroneSnapshot;
import com.infotact.aerosaga.fleet.LegTelemetry;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

/**
 * The mission, written as plain step-by-step code. Temporal makes it durable:
 * if the worker crashes, the mission continues from the last finished step.
 *
 * RULES FOR WORKFLOW CODE (Temporal replays this code to rebuild state):
 *  - No I/O, no threads, no System.currentTimeMillis, no random. Use activities and Workflow.* APIs.
 *  - Same input + same history must always produce the same decisions.
 *
 * SAGA: before each "do" step we register its "undo" step. If anything fails later,
 * saga.compensate() runs the registered undo steps in REVERSE order.
 * We register BEFORE calling the step, because the step may succeed on the server
 * and still look failed to us (timeout). Every undo is safe to run even if the step never happened.
 */
public class MissionWorkflowImpl implements MissionWorkflow {

    private static final Logger log = Workflow.getLogger(MissionWorkflowImpl.class);

    /** Normal steps: retry transient errors a few times, quickly. */
    private final MissionActivities steps = Workflow.newActivityStub(MissionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setMaximumAttempts(5)
                            .build())
                    .build());

    /** Undo steps: must eventually succeed, so retry much longer. */
    private final MissionActivities undo = Workflow.newActivityStub(MissionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setMaximumAttempts(20)
                            .build())
                    .build());

    /** Journal writes are nice-to-have: few retries, and a failure never stops the mission. */
    private final MissionActivities journal = Workflow.newActivityStub(MissionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build());

    // ---- live state (read by the status query) ----
    private String missionId;
    private MissionRequest request;
    private MissionPhase phase = MissionPhase.SUBMITTED;
    private String droneId;
    private String slotId;
    private int legsCompleted;
    private int legsTotal;
    private String nextStop;
    private GeoPoint position;
    private Double batteryPct;
    private double distanceFlownKm;
    private boolean abortRequested;
    private String abortReason;
    private String failureCode;
    private String failureReason;
    private final List<TimelineEntry> timeline = new ArrayList<>();

    /** Thrown inside the workflow to stop the mission when the operator aborts. */
    private static final class AbortRequested extends RuntimeException {
        AbortRequested(String reason) {
            super(reason);
        }
    }

    @Override
    public MissionResult execute(MissionInput input) {
        request = input.request();
        missionId = request.missionId();
        position = request.base();
        Saga saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)   // undo strictly one by one, newest first
                .setContinueWithError(true)       // if one undo fails, still try the others
                .build());
        try {
            enter(MissionPhase.VALIDATING, "Checking plan against operating rules");
            RoutePlan route = steps.planRoute(request);
            legsTotal = route.outboundLegs().size();
            note(String.format("Plan OK: %d stops, %.2f km round trip", legsTotal, route.totalKm()));
            checkAbort();

            enter(MissionPhase.RESERVING_AIRSPACE, "Requesting flight slot");
            saga.addCompensation(() -> {
                note("UNDO: releasing airspace slot");
                undo.releaseAirspace(missionId);
            });
            slotId = steps.reserveAirspace(missionId);
            note("Slot granted: " + slotId);
            checkAbort();

            enter(MissionPhase.ASSIGNING_DRONE, "Finding a drone with enough battery and lift");
            saga.addCompensation(() -> {
                note("UNDO: releasing drone");
                undo.releaseDrone(missionId);
            });
            DroneSnapshot drone = steps.assignDrone(missionId, request.payloadKg(), route.totalKm());
            droneId = drone.droneId();
            batteryPct = drone.batteryPct();
            note("Assigned " + droneId + " (" + drone.model() + ", battery " + drone.batteryPct() + "%)");
            checkAbort();

            enter(MissionPhase.LOADING_PAYLOAD, String.format("Loading %.2f kg", request.payloadKg()));
            saga.addCompensation(() -> {
                note("UNDO: unloading payload");
                undo.unloadPayload(missionId);
            });
            steps.loadPayload(missionId, request.payloadKg());
            checkAbort();

            enter(MissionPhase.IN_FLIGHT, "Take-off");
            saga.addCompensation(() -> {
                note("UNDO: emergency return to base");
                apply(undo.returnToHome(missionId));
            });
            for (RoutePlan.Leg leg : route.outboundLegs()) {
                checkAbort();
                nextStop = leg.toName();
                LegTelemetry t = steps.flyLeg(missionId, leg, request.payloadKg(), request.failureScenario());
                waitFlightTime(t.flightSeconds(), input.timeScale(), true);
                apply(t);
                legsCompleted++;
                note(String.format("Reached %s (%.2f km, battery %.1f%%)", t.reachedName(), t.distanceKm(), t.batteryPct()));
            }
            checkAbort();

            enter(MissionPhase.DELIVERING, "Dropping payload at " + nextStop);
            steps.deliverPayload(missionId);

            enter(MissionPhase.RETURNING, "Flying back to base");
            nextStop = RoutePlan.BASE_NAME;
            LegTelemetry home = steps.returnToHome(missionId);
            waitFlightTime(home.flightSeconds(), input.timeScale(), false);
            apply(home);
            note(String.format("Landed at base, battery %.1f%%", home.batteryPct()));

            steps.releaseDrone(missionId);
            steps.releaseAirspace(missionId);
            return finish(MissionPhase.COMPLETED, "Mission complete");
        } catch (AbortRequested e) {
            failureCode = "OPERATOR_ABORT";
            failureReason = e.getMessage();
            return rollBack(saga, MissionPhase.ABORTED);
        } catch (ActivityFailure e) {
            failureCode = failureType(e);
            failureReason = failureMessage(e);
            if ("INVALID_MISSION".equals(failureCode)) {
                return finish(MissionPhase.REJECTED, failureReason);   // nothing was reserved yet
            }
            return rollBack(saga, MissionPhase.ROLLED_BACK);
        }
    }

    @Override
    public void abort(String reason) {
        if (phase.isFinal() || phase == MissionPhase.DELIVERING
                || phase == MissionPhase.RETURNING || phase == MissionPhase.COMPENSATING) {
            return; // too late to abort: payload already dropped, or already coming home
        }
        abortRequested = true;
        abortReason = (reason == null || reason.isBlank()) ? "Operator abort" : reason;
    }

    @Override
    public MissionView status() {
        List<Waypoint> routeView = new ArrayList<>();
        if (request != null) {
            routeView.add(new Waypoint(RoutePlan.BASE_NAME, request.base()));
            routeView.addAll(request.waypoints());
        }
        return new MissionView(missionId, phase,
                request == null ? FailureScenario.NONE : request.failureScenario(),
                request == null ? 0 : request.payloadKg(),
                droneId, slotId, legsCompleted, legsTotal, nextStop, position, batteryPct,
                round2(distanceFlownKm), abortRequested, failureCode, failureReason,
                routeView, List.copyOf(timeline));
    }

    // ---------------------------------------------------------------------------------------------

    private MissionResult rollBack(Saga saga, MissionPhase outcome) {
        enter(MissionPhase.COMPENSATING, "Undoing finished steps: " + failureReason);
        try {
            saga.compensate();
        } catch (RuntimeException e) {
            log.error("Compensation failed for {}", missionId, e);
            return finish(MissionPhase.COMPENSATION_FAILED, "An undo step failed after all retries. Check the fleet manually.");
        }
        return finish(outcome, outcome == MissionPhase.ABORTED ? "Mission aborted, all steps undone" : "Mission rolled back, all steps undone");
    }

    private MissionResult finish(MissionPhase outcome, String message) {
        nextStop = null;
        enter(outcome, message);
        MissionResult result = new MissionResult(missionId, outcome, droneId, legsCompleted,
                round2(distanceFlownKm), failureCode, failureReason);
        try {
            journal.recordOutcome(result);
        } catch (ActivityFailure e) {
            log.warn("Could not store outcome for {}: {}", missionId, e.getMessage());
        }
        return result;
    }

    /**
     * Simulated flight time. A durable timer: survives worker restarts.
     * If abortable, an abort signal wakes the workflow early.
     */
    private void waitFlightTime(long flightSeconds, double timeScale, boolean abortable) {
        long millis = Math.round(flightSeconds * 1000 * timeScale);
        if (millis <= 0) {
            return;
        }
        if (abortable) {
            Workflow.await(Duration.ofMillis(millis), () -> abortRequested);
        } else {
            Workflow.sleep(Duration.ofMillis(millis));
        }
    }

    private void checkAbort() {
        if (abortRequested) {
            throw new AbortRequested(abortReason);
        }
    }

    private void apply(LegTelemetry t) {
        position = t.position();
        batteryPct = t.batteryPct();
        distanceFlownKm += t.distanceKm();
    }

    private void enter(MissionPhase next, String message) {
        phase = next;
        note(message);
    }

    /** Add a timeline line and copy it to the journal (database). Journal failure never stops the mission. */
    private void note(String message) {
        long now = Workflow.currentTimeMillis();
        timeline.add(new TimelineEntry(now, phase, message));
        log.info("[{}] {} {}", missionId, phase, message);
        try {
            journal.recordEvent(new MissionEvent(missionId, phase, message, droneId, now));
        } catch (ActivityFailure e) {
            log.warn("Journal write failed for {}: {}", missionId, e.getMessage());
        }
    }

    private static String failureType(ActivityFailure e) {
        return (e.getCause() instanceof ApplicationFailure af) ? af.getType() : "ACTIVITY_FAILED";
    }

    private static String failureMessage(ActivityFailure e) {
        if (e.getCause() instanceof ApplicationFailure af) {
            return af.getOriginalMessage();
        }
        Throwable cause = e.getCause() == null ? e : e.getCause();
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
