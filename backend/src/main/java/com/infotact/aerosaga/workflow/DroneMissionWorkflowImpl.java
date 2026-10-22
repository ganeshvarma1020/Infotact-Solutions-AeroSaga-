package com.infotact.aerosaga.workflow;

import com.infotact.aerosaga.activity.MissionActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class DroneMissionWorkflowImpl implements DroneMissionWorkflow {

    private static final Logger log = Workflow.getLogger(DroneMissionWorkflowImpl.class);

    private String currentPhase = "INITIALIZED";
    private int currentStep = 0;
    private boolean abortRequested = false;
    private String abortReason = "";
    private boolean isPaused = false;

    private final MissionActivities activities = Workflow.newActivityStub(
            MissionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(3)
                            .build())
                    .build()
    );

    @Override
    public String executeMission(Long missionId, Long droneId, double destLat, double destLng) {
        log.info("Starting deterministic DroneMissionWorkflow for Mission #{}, Drone #{}", missionId, droneId);
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

        try {
            // 1. Pre-Flight Validation
            currentPhase = "VALIDATION";
            activities.validateMission(missionId);

            // 2. Resource Allocation & Reservation
            currentPhase = "RESOURCE_ALLOCATION";
            activities.reserveDrone(missionId, droneId);

            // Register Saga compensation rollback
            saga.addCompensation(activities::compensateMissionFailure, missionId, droneId, "Emergency Rollback: Flight execution failure or abort command");

            // 3. Execution Loop (5 Waypoint simulation stages)
            int totalSteps = 5;
            for (int step = 1; step <= totalSteps; step++) {
                currentStep = step;
                currentPhase = "TRANSIT_WAYPOINT_" + step;

                // Handle pause signal if received
                if (isPaused) {
                    currentPhase = "HOVER_PAUSED";
                    Workflow.await(() -> !isPaused || abortRequested);
                }

                // Check emergency abort signal
                if (abortRequested) {
                    throw new RuntimeException("Mission aborted by operator: " + abortReason);
                }

                activities.simulateFlightStep(missionId, droneId, step, totalSteps, destLat, destLng);
                Workflow.sleep(Duration.ofMillis(800));
            }

            // 4. Finalization & Asset Release
            currentPhase = "FINALIZING";
            activities.finalizeMission(missionId, droneId);
            currentPhase = "COMPLETED";

            return "SUCCESS: Mission #" + missionId + " completed successfully";

        } catch (Exception e) {
            log.error("Workflow encountered error. Triggering Saga compensation rollback...", e);
            currentPhase = "COMPENSATING_ROLLBACK";
            saga.compensate();
            currentPhase = "FAILED";
            return "FAILED: " + e.getMessage();
        }
    }

    @Override
    public void signalEmergencyAbort(String reason) {
        this.abortRequested = true;
        this.abortReason = reason != null ? reason : "Manual abort command received from operator";
        log.warn("Emergency abort signal received: {}", this.abortReason);
    }

    @Override
    public void signalPause(int seconds) {
        this.isPaused = true;
        log.info("Pause signal received. Pausing mission for {} seconds", seconds);
    }

    @Override
    public void signalResume() {
        this.isPaused = false;
        log.info("Resume signal received. Continuing flight path.");
    }

    @Override
    public String queryCurrentPhase() {
        return currentPhase;
    }

    @Override
    public int queryCurrentStep() {
        return currentStep;
    }

    @Override
    public boolean isAborted() {
        return abortRequested;
    }
}
