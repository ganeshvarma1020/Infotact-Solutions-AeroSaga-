package com.infotact.aerosaga.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.infotact.aerosaga.TestData;
import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.MissionPlanValidator;
import com.infotact.aerosaga.domain.MissionRequest;
import com.infotact.aerosaga.domain.OperatingLimits;
import com.infotact.aerosaga.domain.RoutePlan;
import com.infotact.aerosaga.fleet.DroneFleet;
import com.infotact.aerosaga.fleet.DroneSnapshot;
import com.infotact.aerosaga.fleet.DroneStatus;
import com.infotact.aerosaga.fleet.FlightSimulator;
import com.infotact.aerosaga.fleet.LegTelemetry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;

/**
 * Runs the real workflow and real activities against Temporal's in-memory test server.
 * Timers are skipped automatically, so a 20-minute simulated flight finishes in milliseconds.
 */
class MissionWorkflowTest {

    private static final String QUEUE = "test-missions";

    /** Counts flyLeg calls so the retry test can prove Temporal retried. */
    static class CountingSimulator extends FlightSimulator {
        final AtomicInteger flyLegCalls = new AtomicInteger();

        CountingSimulator(DroneFleet fleet, AirspaceRegistry airspace, OperatingLimits limits) {
            super(fleet, airspace, limits);
        }

        @Override
        public LegTelemetry flyLeg(String droneId, String missionId, RoutePlan.Leg leg, double payloadKg,
                                   FailureScenario scenario, int attempt) {
            flyLegCalls.incrementAndGet();
            return super.flyLeg(droneId, missionId, leg, payloadKg, scenario, attempt);
        }
    }

    private TestWorkflowEnvironment env;
    private WorkflowClient client;
    private DroneFleet fleet;
    private AirspaceRegistry airspace;
    private CountingSimulator sim;
    private InMemoryJournal journal;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        fleet = new DroneFleet(TestData.BASE, DroneFleet.demoSpecs());
        airspace = new AirspaceRegistry(3);
        sim = new CountingSimulator(fleet, airspace, TestData.LIMITS);
        journal = new InMemoryJournal();
        Worker worker = env.newWorker(QUEUE);
        worker.registerWorkflowImplementationTypes(MissionWorkflowImpl.class);
        worker.registerActivitiesImplementations(new MissionActivitiesImpl(
                new MissionPlanValidator(TestData.LIMITS), TestData.LIMITS, fleet, airspace, sim, journal));
        env.start();
        client = env.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private MissionWorkflow stub(String id) {
        return client.newWorkflowStub(MissionWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(id).setTaskQueue(QUEUE).build());
    }

    private MissionResult run(MissionRequest request) {
        return stub(request.missionId()).execute(new MissionInput(request, 1.0));
    }

    private void assertFleetAndAirspaceClean() {
        for (DroneSnapshot d : fleet.list()) {
            assertEquals(DroneStatus.AVAILABLE, d.status(), d.droneId() + " not released");
            assertEquals(TestData.BASE, d.position(), d.droneId() + " not at base");
            assertEquals(0.0, d.payloadKg(), 1e-9);
        }
        assertEquals(0, airspace.inUse(), "airspace slot leaked");
    }

    @Test
    void happyPathCompletesAndReleasesEverything() {
        MissionResult r = run(TestData.mission("M-OK"));

        assertEquals(MissionPhase.COMPLETED, r.outcome());
        assertEquals(2, r.legsCompleted());
        assertEquals("DR-03", r.droneId());
        assertTrue(r.distanceFlownKm() > 16 && r.distanceFlownKm() < 18, "distance " + r.distanceFlownKm());
        assertFleetAndAirspaceClean();
        double battery = fleet.find("DR-03").orElseThrow().batteryPct();
        assertTrue(battery < 100 && battery >= TestData.LIMITS.batteryReservePct(), "battery " + battery);
        assertEquals(-1, journal.indexOf("M-OK", "UNDO"), "no undo on success");
        assertEquals(MissionPhase.COMPLETED, journal.outcomes().get(0).outcome());
    }

    @Test
    void lowBatteryRollsBackInReverseOrder() {
        MissionResult r = run(TestData.mission("M-LB", 1.0, FailureScenario.LOW_BATTERY));

        assertEquals(MissionPhase.ROLLED_BACK, r.outcome());
        assertEquals("LOW_BATTERY", r.failureCode());
        int home = journal.indexOf("M-LB", "UNDO: emergency return");
        int unload = journal.indexOf("M-LB", "UNDO: unloading payload");
        int drone = journal.indexOf("M-LB", "UNDO: releasing drone");
        int slot = journal.indexOf("M-LB", "UNDO: releasing airspace");
        assertTrue(home >= 0 && home < unload && unload < drone && drone < slot,
                "undo order wrong: " + journal.messages("M-LB"));
        assertFleetAndAirspaceClean();
    }

    @Test
    void geofenceBreachRollsBackBeforeDroneMoves() {
        MissionResult r = run(TestData.mission("M-GEO", 1.0, FailureScenario.GEOFENCE_BREACH));

        assertEquals(MissionPhase.ROLLED_BACK, r.outcome());
        assertEquals("GEOFENCE_BREACH", r.failureCode());
        assertEquals(0, r.legsCompleted());
        assertEquals(0.0, r.distanceFlownKm(), 1e-9);
        assertFalse(airspace.temporaryZones().isEmpty());
        assertFleetAndAirspaceClean();
    }

    @Test
    void flakyTelemetryIsRetriedAndMissionStillCompletes() {
        MissionResult r = run(TestData.mission("M-FLAKY", 1.0, FailureScenario.FLAKY_TELEMETRY));

        assertEquals(MissionPhase.COMPLETED, r.outcome());
        // leg 0: attempts 1 and 2 fail, 3 succeeds; leg 1: one call
        assertEquals(4, sim.flyLegCalls.get());
        assertFleetAndAirspaceClean();
    }

    @Test
    void invalidPlanIsRejectedWithNothingToUndo() {
        MissionRequest bad = new MissionRequest("M-BAD", "op", TestData.BASE, List.of(), 1.0, FailureScenario.NONE);
        MissionResult r = run(bad);

        assertEquals(MissionPhase.REJECTED, r.outcome());
        assertEquals("INVALID_MISSION", r.failureCode());
        assertEquals(-1, journal.indexOf("M-BAD", "UNDO"));
        assertFleetAndAirspaceClean();
    }

    @Test
    void noSuitableDroneReleasesTheAirspaceSlot() {
        // 4 kg over the full route: no drone has enough battery (see DroneFleetTest)
        MissionResult r = run(TestData.mission("M-HEAVY", 4.0, FailureScenario.NONE));

        assertEquals(MissionPhase.ROLLED_BACK, r.outcome());
        assertEquals("NO_DRONE_AVAILABLE", r.failureCode());
        assertTrue(journal.indexOf("M-HEAVY", "UNDO: releasing airspace") >= 0);
        assertFleetAndAirspaceClean();
    }

    @Test
    void operatorAbortMidFlightBringsDroneHome() {
        MissionWorkflow wf = stub("M-ABORT");
        WorkflowClient.start(wf::execute, new MissionInput(TestData.mission("M-ABORT"), 1.0));

        MissionView live = null;
        for (int i = 0; i < 100; i++) {
            try {
                live = wf.status();
                if (live.phase() == MissionPhase.IN_FLIGHT) {
                    break;
                }
            } catch (RuntimeException notReadyYet) {
                // the first workflow task may not have run yet; try again
            }
            env.sleep(Duration.ofSeconds(1));
        }
        assertNotNull(live);
        assertEquals(MissionPhase.IN_FLIGHT, live.phase(), "never reached IN_FLIGHT");
        assertNotNull(live.droneId());
        assertNotNull(live.slotId());

        wf.abort("Bird strike reported");
        MissionResult r = WorkflowStub.fromTyped(wf).getResult(MissionResult.class);

        assertEquals(MissionPhase.ABORTED, r.outcome());
        assertEquals("OPERATOR_ABORT", r.failureCode());
        assertEquals("Bird strike reported", r.failureReason());
        assertFleetAndAirspaceClean();
    }

    @Test
    void historyReplaysDeterministically() throws Exception {
        run(TestData.mission("M-REPLAY", 1.0, FailureScenario.LOW_BATTERY));
        WorkflowExecutionHistory history = client.fetchHistory("M-REPLAY");
        // Throws if the workflow code makes different decisions on replay (non-determinism bug)
        WorkflowReplayer.replayWorkflowExecution(history, MissionWorkflowImpl.class);
    }
}
