package com.infotact.aerosaga.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.infotact.aerosaga.TestData;
import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.MissionException;
import com.infotact.aerosaga.domain.MissionException.Code;
import com.infotact.aerosaga.domain.RoutePlan;

class FlightSimulatorTest {

    private static final String MISSION = "M-1";
    private static final double PAYLOAD = 1.0;

    private DroneFleet fleet;
    private AirspaceRegistry airspace;
    private FlightSimulator sim;
    private RoutePlan route;
    private String drone;

    @BeforeEach
    void setUp() {
        fleet = new DroneFleet(TestData.BASE, DroneFleet.demoSpecs());
        airspace = new AirspaceRegistry(3);
        sim = new FlightSimulator(fleet, airspace, TestData.LIMITS);
        route = TestData.mission(MISSION).route();
        drone = fleet.assign(MISSION, PAYLOAD, route.totalKm(), TestData.LIMITS.batteryReservePct()).droneId();
    }

    @Test
    void normalLegMovesDroneAndDrainsExpectedBattery() {
        RoutePlan.Leg leg = route.legs().get(0);
        double expectedUse = BatteryModel.percentFor(fleet.spec(drone), PAYLOAD, leg.distanceKm());

        LegTelemetry t = sim.flyLeg(drone, MISSION, leg, PAYLOAD, FailureScenario.NONE, 1);

        assertEquals(leg.toName(), t.reachedName());
        assertEquals(100.0 - expectedUse, t.batteryPct(), 0.06);
        DroneSnapshot s = fleet.find(drone).orElseThrow();
        assertEquals(leg.to(), s.position());
        assertEquals(DroneStatus.IN_FLIGHT, s.status());
        assertTrue(t.flightSeconds() > 0);
    }

    @Test
    void wholeNormalRouteNeverTriggersLowBattery() {
        for (RoutePlan.Leg leg : route.outboundLegs()) {
            sim.flyLeg(drone, MISSION, leg, PAYLOAD, FailureScenario.NONE, 1);
        }
        LegTelemetry home = sim.returnToHome(drone, MISSION, 0);
        assertTrue(home.batteryPct() >= TestData.LIMITS.batteryReservePct(),
                "landed with " + home.batteryPct() + "%");
    }

    @Test
    void cellFaultTriggersLowBatteryAbort() {
        MissionException e = assertThrows(MissionException.class,
                () -> sim.flyLeg(drone, MISSION, route.legs().get(0), PAYLOAD, FailureScenario.LOW_BATTERY, 1));
        assertEquals(Code.LOW_BATTERY, e.code());
        // The drone did fly the leg before noticing: it is now away from base and must be brought home
        assertEquals(route.legs().get(0).to(), fleet.find(drone).orElseThrow().position());
    }

    @Test
    void flakyTelemetryFailsTwiceWithoutSideEffectsThenSucceeds() {
        RoutePlan.Leg leg = route.legs().get(0);
        for (int attempt = 1; attempt <= 2; attempt++) {
            final int a = attempt;
            MissionException e = assertThrows(MissionException.class,
                    () -> sim.flyLeg(drone, MISSION, leg, PAYLOAD, FailureScenario.FLAKY_TELEMETRY, a));
            assertEquals(Code.TELEMETRY_LOST, e.code());
            DroneSnapshot s = fleet.find(drone).orElseThrow();
            assertEquals(TestData.BASE, s.position());
            assertEquals(100.0, s.batteryPct(), 1e-9);
        }
        LegTelemetry t = sim.flyLeg(drone, MISSION, leg, PAYLOAD, FailureScenario.FLAKY_TELEMETRY, 3);
        assertEquals(leg.toName(), t.reachedName());
    }

    @Test
    void geofenceBreachStopsDroneBeforeItMoves() {
        MissionException e = assertThrows(MissionException.class,
                () -> sim.flyLeg(drone, MISSION, route.legs().get(0), PAYLOAD, FailureScenario.GEOFENCE_BREACH, 1));
        assertEquals(Code.GEOFENCE_BREACH, e.code());
        assertEquals(TestData.BASE, fleet.find(drone).orElseThrow().position());
        assertEquals("TFR-" + MISSION, airspace.temporaryZones().get(0).name());
    }

    @Test
    void returnToHomeLandsAtBaseStillAssigned() {
        sim.flyLeg(drone, MISSION, route.legs().get(0), PAYLOAD, FailureScenario.NONE, 1);
        LegTelemetry t = sim.returnToHome(drone, MISSION, PAYLOAD);
        DroneSnapshot s = fleet.find(drone).orElseThrow();
        assertEquals(TestData.BASE, s.position());
        assertEquals(DroneStatus.ASSIGNED, s.status());
        assertEquals(RoutePlan.BASE_NAME, t.reachedName());
    }
}
