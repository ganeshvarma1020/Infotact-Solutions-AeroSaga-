package com.infotact.aerosaga;

import com.infotact.aerosaga.activity.*;
import com.infotact.aerosaga.model.*;
import com.infotact.aerosaga.workflow.DroneMissionWorkflow;
import com.infotact.aerosaga.workflow.DroneMissionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DroneMissionWorkflowTest {

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient workflowClient;

    private static final String TASK_QUEUE = "TestDroneQueue";

    // Test activity implementations to avoid Mockito proxy annotation reflection conflicts
    private static class TestPreFlightActivities implements PreFlightActivities {
        @Override
        public boolean verifyBattery(String droneId, double requiredMinimumPercent) {
            return true;
        }

        @Override
        public WeatherReport evaluateAirspaceWeather(double latitude, double longitude) {
            return new WeatherReport(true, 8.0, 10.0, 10.0, 0.0, false, "Nominal VFR");
        }

        @Override
        public boolean verifyAirspaceClearance(List<Waypoint> waypoints) {
            return true;
        }

        @Override
        public boolean calibrateSensorsAndMotors(String droneId) {
            return true;
        }
    }

    private static class TestFlightControlActivities implements FlightControlActivities {
        final AtomicBoolean takeoffCalled = new AtomicBoolean(false);
        final AtomicBoolean landingCalled = new AtomicBoolean(false);
        final AtomicInteger waypointsVisited = new AtomicInteger(0);

        @Override
        public void initiateTakeoff(String droneId, String missionId, double targetAltitudeMeters) {
            takeoffCalled.set(true);
        }

        @Override
        public DroneTelemetry flyToWaypoint(String droneId, String missionId, Waypoint waypoint, int waypointIndex, int totalWaypoints) {
            waypointsVisited.incrementAndGet();
            return new DroneTelemetry(droneId, missionId, waypoint.getLatitude(), waypoint.getLongitude(),
                    waypoint.getAltitudeMeters(), waypoint.getSpeedMps(), 45.0, 95.0, 24.0, 40.0, 16, 6.0,
                    DroneStatus.AIRBORNE, waypoint.getLabel());
        }

        @Override
        public void hoverInPlace(String droneId, String missionId, int seconds) {
        }

        @Override
        public void returnToBase(String droneId, String missionId, Waypoint baseWaypoint) {
        }

        @Override
        public void initiateLanding(String droneId, String missionId) {
            landingCalled.set(true);
        }
    }

    private static class TestPayloadActivities implements PayloadActivities {
        final AtomicBoolean payloadDeployed = new AtomicBoolean(false);

        @Override
        public boolean deployDeliveryPayload(String droneId, String missionId, double payloadWeightKg) {
            payloadDeployed.set(true);
            return true;
        }

        @Override
        public boolean executeSurveillanceScan(String droneId, String missionId, String targetLabel) {
            return true;
        }
    }

    private static class TestEmergencyActivities implements EmergencyActivities {
        final AtomicBoolean touchdownCompensated = new AtomicBoolean(false);
        final AtomicBoolean payloadLocked = new AtomicBoolean(false);
        final AtomicBoolean beaconBroadcasted = new AtomicBoolean(false);
        final AtomicBoolean auditRecorded = new AtomicBoolean(false);

        @Override
        public void executeEmergencySafeTouchdown(String droneId, String missionId, String failureReason) {
            touchdownCompensated.set(true);
        }

        @Override
        public void securePayloadLockdown(String droneId, String missionId) {
            payloadLocked.set(true);
        }

        @Override
        public void broadcastAirspaceEmergencyBeacon(String droneId, String missionId, String emergencyReason) {
            beaconBroadcasted.set(true);
        }

        @Override
        public void recordMissionCompensation(String missionId, String compensationDetails) {
            auditRecorded.set(true);
        }
    }

    private TestFlightControlActivities flightActivities;
    private TestPayloadActivities payloadActivities;
    private TestEmergencyActivities emergencyActivities;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        workflowClient = testEnv.getWorkflowClient();
        worker = testEnv.newWorker(TASK_QUEUE);

        flightActivities = new TestFlightControlActivities();
        payloadActivities = new TestPayloadActivities();
        emergencyActivities = new TestEmergencyActivities();

        worker.registerWorkflowImplementationTypes(DroneMissionWorkflowImpl.class);
        worker.registerActivitiesImplementations(
                new TestPreFlightActivities(),
                flightActivities,
                payloadActivities,
                emergencyActivities
        );

        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    @DisplayName("Verify successful end-to-end mission execution with Temporal workflow")
    void testSuccessfulMissionExecution() {
        MissionRequest request = new MissionRequest(
                "DRONE-ALPHA-01",
                MissionType.MEDICAL_SUPPLY_DELIVERY,
                List.of(
                        new Waypoint(12.9716, 80.2446, 30.0, 10.0, "Base Pad"),
                        new Waypoint(12.9780, 80.2510, 40.0, 12.0, "Hospital Drop Zone")
                ),
                2.5
        );

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId("TEST-MSN-SUCCESS-01")
                .build();

        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, options);
        MissionResult result = workflow.executeMission(request);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Mission result should be successful");
        assertEquals(MissionStatus.COMPLETED, result.getStatus());
        assertEquals(2, result.getWaypointsCompleted());
        assertFalse(result.isSagaCompensated());

        assertTrue(flightActivities.takeoffCalled.get(), "Takeoff activity should have executed");
        assertTrue(payloadActivities.payloadDeployed.get(), "Payload delivery activity should have executed");
        assertTrue(flightActivities.landingCalled.get(), "Landing activity should have executed");
    }

    @Test
    @DisplayName("Verify Saga compensation rollback triggers upon severe weather fault")
    void testSevereWeatherSagaCompensation() {
        MissionRequest request = new MissionRequest(
                "DRONE-GAMMA-03",
                MissionType.MEDICAL_SUPPLY_DELIVERY,
                List.of(
                        new Waypoint(12.9716, 80.2446, 30.0, 10.0, "Base Pad"),
                        new Waypoint(12.9780, 80.2510, 40.0, 12.0, "Target")
                ),
                1.5
        );
        request.setSimulateSevereWeather(true); // Triggers in-flight failure exception

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId("TEST-MSN-SAGA-COMPENSATE-02")
                .build();

        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, options);
        MissionResult result = workflow.executeMission(request);

        assertNotNull(result);
        assertFalse(result.isSuccess(), "Mission should fail due to severe weather");
        assertEquals(MissionStatus.ABORTED_SAFE_LANDING, result.getStatus());
        assertTrue(result.isSagaCompensated(), "Saga compensation should be executed");

        // Verify compensation activities were executed by Saga
        assertTrue(emergencyActivities.touchdownCompensated.get(), "Emergency touchdown compensation should have executed");
        assertTrue(emergencyActivities.payloadLocked.get(), "Payload lockdown compensation should have executed");
        assertTrue(emergencyActivities.beaconBroadcasted.get(), "Emergency beacon compensation should have executed");
        assertTrue(emergencyActivities.auditRecorded.get(), "Audit record compensation should have executed");
    }
}
