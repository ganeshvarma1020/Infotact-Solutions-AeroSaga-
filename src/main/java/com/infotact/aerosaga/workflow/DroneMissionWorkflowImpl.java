package com.infotact.aerosaga.workflow;

import com.infotact.aerosaga.activity.EmergencyActivities;
import com.infotact.aerosaga.activity.FlightControlActivities;
import com.infotact.aerosaga.activity.PayloadActivities;
import com.infotact.aerosaga.activity.PreFlightActivities;
import com.infotact.aerosaga.model.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Temporal Workflow Implementation governing drone flight operations with Saga compensation.
 */
public class DroneMissionWorkflowImpl implements DroneMissionWorkflow {

    private static final Logger logger = Workflow.getLogger(DroneMissionWorkflowImpl.class);

    private final ActivityOptions defaultActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(45))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final PreFlightActivities preFlightActivities =
            Workflow.newActivityStub(PreFlightActivities.class, defaultActivityOptions);

    private final FlightControlActivities flightControlActivities =
            Workflow.newActivityStub(FlightControlActivities.class, defaultActivityOptions);

    private final PayloadActivities payloadActivities =
            Workflow.newActivityStub(PayloadActivities.class, defaultActivityOptions);

    private final EmergencyActivities emergencyActivities =
            Workflow.newActivityStub(EmergencyActivities.class, defaultActivityOptions);

    // Workflow state variables
    private MissionStatus currentStatus = MissionStatus.PENDING;
    private String currentPhase = "Initialized";
    private DroneTelemetry currentTelemetry = new DroneTelemetry();
    private List<Waypoint> activeWaypoints = new ArrayList<>();
    private boolean abortRequested = false;
    private String abortReason = null;
    private boolean pauseRequested = false;
    private int pauseSeconds = 0;
    private boolean sagaCompensated = false;

    @Override
    public MissionResult executeMission(MissionRequest request) {
        String missionId = Workflow.getInfo().getWorkflowId();
        String droneId = request.getDroneId();
        long startTime = Workflow.currentTimeMillis();
        int waypointsCompleted = 0;
        double totalDistanceKm = 0.0;

        // Initialize active waypoints
        if (request.getWaypoints() != null) {
            this.activeWaypoints = new ArrayList<>(request.getWaypoints());
        }

        // Initialize Temporal Saga compensation manager
        Saga.Options sagaOptions = new Saga.Options.Builder()
                .setParallelCompensation(false)
                .build();
        Saga saga = new Saga(sagaOptions);

        try {
            // ==========================================
            // PHASE 1: PRE-FLIGHT CHECKS & DIAGNOSTICS
            // ==========================================
            currentStatus = MissionStatus.PREFLIGHT_INSPECTION;
            currentPhase = "Pre-flight checks: Battery, Sensors, Airspace & Meteorology";
            logger.info("Executing Phase 1: Pre-Flight inspection for drone {}", droneId);

            // 1. Verify Battery
            preFlightActivities.verifyBattery(droneId, 30.0);

            // 2. Weather Assessment
            Waypoint initialWaypoint = activeWaypoints.isEmpty() ?
                    new Waypoint(12.9716, 80.2446, 0, 0, "Base") : activeWaypoints.get(0);
            preFlightActivities.evaluateAirspaceWeather(initialWaypoint.getLatitude(), initialWaypoint.getLongitude());

            // 3. Airspace Geofence Clearance
            preFlightActivities.verifyAirspaceClearance(activeWaypoints);

            // 4. Avionics and Motor Diagnostics
            preFlightActivities.calibrateSensorsAndMotors(droneId);

            // Check if abort was requested during preflight
            if (abortRequested) {
                throw new RuntimeException("Mission aborted by command center prior to takeoff: " + abortReason);
            }

            // ==========================================
            // PHASE 2: TAKEOFF & ASCENT
            // ==========================================
            currentStatus = MissionStatus.TAKEOFF;
            currentPhase = "Ascending to cruise altitude";
            logger.info("Executing Phase 2: Takeoff for drone {}", droneId);

            flightControlActivities.initiateTakeoff(droneId, missionId, 35.0);

            // Register compensating actions with the Saga once drone is airborne
            saga.addCompensation(() -> {
                emergencyActivities.executeEmergencySafeTouchdown(droneId, missionId, abortReason != null ? abortReason : "Autonomous Fail-safe Triggered");
                emergencyActivities.securePayloadLockdown(droneId, missionId);
                emergencyActivities.broadcastAirspaceEmergencyBeacon(droneId, missionId, abortReason != null ? abortReason : "Fail-safe");
                emergencyActivities.recordMissionCompensation(missionId, abortReason != null ? abortReason : "Mission Compensated");
            });

            // Simulated mid-air failure checks for demo evaluation
            if (request.isSimulateSevereWeather()) {
                throw new RuntimeException("In-flight meteorological alert: 50kt violent gusts detected! Airspace compromised.");
            }
            if (request.isSimulateMotorFailure()) {
                throw new RuntimeException("Critical avionics failure: Rotor #3 RPM variance detected outside tolerance threshold!");
            }

            // ==========================================
            // PHASE 3: WAYPOINT NAVIGATION EN-ROUTE
            // ==========================================
            currentStatus = MissionStatus.EN_ROUTE;
            currentPhase = "Navigating autonomous flight trajectory";
            logger.info("Executing Phase 3: Flight navigation along {} waypoints", activeWaypoints.size());

            Waypoint previousPoint = initialWaypoint;
            for (int i = 0; i < activeWaypoints.size(); i++) {
                if (abortRequested) {
                    throw new RuntimeException("Emergency abort signaled during waypoint transit: " + abortReason);
                }

                // Handle pause/hover signal if requested
                if (pauseRequested) {
                    currentPhase = "Stationary Hover Hold (" + pauseSeconds + "s)";
                    flightControlActivities.hoverInPlace(droneId, missionId, pauseSeconds);
                    pauseRequested = false;
                }

                Waypoint currentTarget = activeWaypoints.get(i);
                currentPhase = "Transiting towards " + currentTarget.getLabel();
                currentTelemetry = flightControlActivities.flyToWaypoint(droneId, missionId, currentTarget, i, activeWaypoints.size());
                waypointsCompleted++;

                // Track distance
                totalDistanceKm += com.infotact.aerosaga.service.GeofenceService.calculateHaversineDistanceKm(
                        previousPoint.getLatitude(), previousPoint.getLongitude(),
                        currentTarget.getLatitude(), currentTarget.getLongitude()
                );
                previousPoint = currentTarget;

                // Brief inter-waypoint check
                Workflow.sleep(Duration.ofMillis(300));
            }

            // ==========================================
            // PHASE 4: PAYLOAD / OBJECTIVE EXECUTION
            // ==========================================
            if (request.getMissionType() == MissionType.MEDICAL_SUPPLY_DELIVERY) {
                currentStatus = MissionStatus.DELIVERING_PAYLOAD;
                currentPhase = "Deploying medical payload package";
                payloadActivities.deployDeliveryPayload(droneId, missionId, request.getPayloadWeightKg());
            } else {
                currentStatus = MissionStatus.SURVEILLANCE_ACTIVE;
                currentPhase = "Conducting optical 4K & LiDAR sensor surveillance scan";
                payloadActivities.executeSurveillanceScan(droneId, missionId, "Target Perimeter");
            }

            // ==========================================
            // PHASE 5: RETURN TO BASE (RTB) & LANDING
            // ==========================================
            currentStatus = MissionStatus.RETURNING_TO_BASE;
            currentPhase = "Autonomous Return-to-Base (RTB)";
            logger.info("Executing Phase 5: RTB for drone {}", droneId);

            flightControlActivities.returnToBase(droneId, missionId, initialWaypoint);

            currentStatus = MissionStatus.LANDING;
            currentPhase = "Landing and post-flight engine shutdown";
            flightControlActivities.initiateLanding(droneId, missionId);

            currentStatus = MissionStatus.COMPLETED;
            currentPhase = "Mission Successfully Completed";

            long durationSeconds = (Workflow.currentTimeMillis() - startTime) / 1000;
            return new MissionResult(
                    missionId,
                    droneId,
                    currentStatus,
                    true,
                    "Mission completed successfully without incident.",
                    waypointsCompleted,
                    Math.round(totalDistanceKm * 100.0) / 100.0,
                    currentTelemetry.getBatteryPercentage(),
                    durationSeconds,
                    false
            );

        } catch (Exception e) {
            logger.error("Mission failed or aborted: {}. Initiating Saga compensation...", e.getMessage());
            this.abortReason = e.getMessage();
            this.currentStatus = MissionStatus.ABORTED_SAFE_LANDING;
            this.currentPhase = "Saga Compensating: Safe Touchdown & Airspace Clearance";

            // Execute Saga compensation activities
            saga.compensate();
            this.sagaCompensated = true;

            long durationSeconds = (Workflow.currentTimeMillis() - startTime) / 1000;
            return new MissionResult(
                    missionId,
                    droneId,
                    currentStatus,
                    false,
                    "Mission aborted due to safety exception: " + e.getMessage() + ". Saga compensation executed.",
                    waypointsCompleted,
                    Math.round(totalDistanceKm * 100.0) / 100.0,
                    currentTelemetry != null ? currentTelemetry.getBatteryPercentage() : 0.0,
                    durationSeconds,
                    true
            );
        }
    }

    @Override
    public void signalReroute(RerouteRequest reroute) {
        logger.info("Received Signal: signalReroute with {} new waypoints. Reason: {}",
                reroute.getNewWaypoints().size(), reroute.getReason());
        if (reroute.getNewWaypoints() != null && !reroute.getNewWaypoints().isEmpty()) {
            this.activeWaypoints = new ArrayList<>(reroute.getNewWaypoints());
            this.currentPhase = "Rerouting active: " + reroute.getReason();
        }
    }

    @Override
    public void signalPauseHover(int seconds) {
        logger.info("Received Signal: signalPauseHover for {} seconds", seconds);
        this.pauseRequested = true;
        this.pauseSeconds = seconds;
    }

    @Override
    public void signalResume() {
        logger.info("Received Signal: signalResume flight transit");
        this.pauseRequested = false;
    }

    @Override
    public void signalEmergencyAbort(String reason) {
        logger.warn("Received Signal: signalEmergencyAbort with reason: {}", reason);
        this.abortRequested = true;
        this.abortReason = reason;
    }

    @Override
    public MissionStatus queryMissionStatus() {
        return currentStatus;
    }

    @Override
    public DroneTelemetry queryCurrentTelemetry() {
        return currentTelemetry;
    }

    @Override
    public String queryCurrentPhase() {
        return currentPhase;
    }

    @Override
    public List<Waypoint> queryActiveWaypoints() {
        return Collections.unmodifiableList(activeWaypoints);
    }
}
