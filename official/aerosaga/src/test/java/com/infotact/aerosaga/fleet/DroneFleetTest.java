package com.infotact.aerosaga.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.infotact.aerosaga.TestData;
import com.infotact.aerosaga.domain.MissionException;
import com.infotact.aerosaga.domain.RoutePlan;

class DroneFleetTest {

    private static final double RESERVE = TestData.LIMITS.batteryReservePct();
    private DroneFleet fleet;
    private double demoRouteKm;
    private double shortRouteKm;

    @BeforeEach
    void setUp() {
        fleet = new DroneFleet(TestData.BASE, DroneFleet.demoSpecs());
        demoRouteKm = TestData.mission("M").route().totalKm();
        shortRouteKm = RoutePlan.of(TestData.BASE, List.of(TestData.BOMMASANDRA)).totalKm();
    }

    @Test
    void batteryModelChargesMoreForHeavierPayload() {
        DroneSpec spec = fleet.spec("DR-02");
        assertEquals(4.0, BatteryModel.percentPerKm(spec, 0), 1e-9);    // 100 / 25 km
        assertEquals(6.0, BatteryModel.percentPerKm(spec, 5.0), 1e-9);  // +50% at max payload
    }

    @Test
    void picksDroneWithMostSpareBattery() {
        // 1 kg over ~16.8 km: DR-03 (45 km range) has by far the most spare battery
        DroneSnapshot d = fleet.assign("M-1", 1.0, demoRouteKm, RESERVE);
        assertEquals("DR-03", d.droneId());
        assertEquals(DroneStatus.ASSIGNED, d.status());
        assertEquals("M-1", d.missionId());
    }

    @Test
    void heavyPayloadGoesToCargoDrone() {
        DroneSnapshot d = fleet.assign("M-1", 4.0, shortRouteKm, RESERVE);
        assertEquals("DR-02", d.droneId());
    }

    @Test
    void assignIsIdempotentForSameMission() {
        DroneSnapshot first = fleet.assign("M-1", 1.0, demoRouteKm, RESERVE);
        DroneSnapshot again = fleet.assign("M-1", 1.0, demoRouteKm, RESERVE);
        assertEquals(first.droneId(), again.droneId());
        long booked = fleet.list().stream().filter(s -> s.status() == DroneStatus.ASSIGNED).count();
        assertEquals(1, booked);
    }

    @Test
    void failsWithReasonWhenNoDroneFits() {
        // 4 kg over the full demo route needs ~94% + 20% reserve on DR-02: impossible
        MissionException e = assertThrows(MissionException.class,
                () -> fleet.assign("M-1", 4.0, demoRouteKm, RESERVE));
        assertEquals(MissionException.Code.NO_DRONE_AVAILABLE, e.code());
        assertTrue(e.getMessage().contains("DR-01: payload too heavy"));
    }

    @Test
    void busyDronesAreSkipped() {
        fleet.assign("M-1", 1.0, demoRouteKm, RESERVE);
        fleet.assign("M-2", 1.0, demoRouteKm, RESERVE);
        fleet.assign("M-3", 1.0, demoRouteKm, RESERVE);
        MissionException e = assertThrows(MissionException.class,
                () -> fleet.assign("M-4", 1.0, demoRouteKm, RESERVE));
        assertTrue(e.getMessage().contains("busy"));
    }

    @Test
    void releaseIsIdempotentAndRespectsOwnership() {
        String drone = fleet.assign("M-1", 1.0, demoRouteKm, RESERVE).droneId();
        fleet.release(drone, "M-OTHER");                 // wrong mission: ignored
        assertEquals("M-1", fleet.find(drone).orElseThrow().missionId());
        fleet.release(drone, "M-1");
        fleet.release(drone, "M-1");                     // second call: no error
        DroneSnapshot s = fleet.find(drone).orElseThrow();
        assertEquals(DroneStatus.AVAILABLE, s.status());
        assertNull(s.missionId());
    }

    @Test
    void cannotChangeDroneOwnedByAnotherMission() {
        String drone = fleet.assign("M-1", 1.0, demoRouteKm, RESERVE).droneId();
        assertThrows(IllegalStateException.class, () -> fleet.loadPayload(drone, "M-2", 1.0));
    }

    @Test
    void rejectsDuplicateDroneIds() {
        DroneSpec s = DroneFleet.demoSpecs().get(0);
        assertThrows(IllegalArgumentException.class, () -> new DroneFleet(TestData.BASE, List.of(s, s)));
    }
}
