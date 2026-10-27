package com.infotact.aerosaga.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.infotact.aerosaga.TestData;
import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.NoFlyZone;
import com.infotact.aerosaga.fleet.DroneFleet;
import com.infotact.aerosaga.fleet.DroneSnapshot;
import com.infotact.aerosaga.fleet.DroneStatus;

class SimulationStateCodecTest {

    @Test
    void roundTripKeepsEveryField() {
        DroneFleet fleet = new DroneFleet(TestData.BASE, DroneFleet.demoSpecs());
        AirspaceRegistry airspace = new AirspaceRegistry(3);
        String drone = fleet.assign("M-1", 1.0, 10, 20).droneId();
        fleet.loadPayload(drone, "M-1", 1.0);
        fleet.recordFlight(drone, "M-1", TestData.BOMMASANDRA.position(), 12.5);
        airspace.reserve("M-1");
        airspace.addTemporaryZone(new NoFlyZone("TFR|odd name\nwith newline", new GeoPoint(12.9, 77.6), 0.5));

        SimulationState before = new SimulationState(fleet.list(), airspace.slots(), airspace.temporaryZones());
        SimulationState after = SimulationStateCodec.decode(SimulationStateCodec.encode(before));

        assertEquals(before, after);
    }

    @Test
    void restoreRebuildsFleetAndAirspace() {
        DroneFleet original = new DroneFleet(TestData.BASE, DroneFleet.demoSpecs());
        AirspaceRegistry originalAir = new AirspaceRegistry(3);
        String drone = original.assign("M-1", 1.0, 10, 20).droneId();
        originalAir.reserve("M-1");
        String text = SimulationStateCodec.encode(
                new SimulationState(original.list(), originalAir.slots(), originalAir.temporaryZones()));

        // "App restart": brand-new objects, then restore
        DroneFleet restarted = new DroneFleet(TestData.BASE, DroneFleet.demoSpecs());
        AirspaceRegistry restartedAir = new AirspaceRegistry(3);
        SimulationState saved = SimulationStateCodec.decode(text);
        restarted.restore(saved.drones());
        restartedAir.restore(saved.slots(), saved.zones());

        DroneSnapshot s = restarted.find(drone).orElseThrow();
        assertEquals(DroneStatus.ASSIGNED, s.status());
        assertEquals("M-1", s.missionId());
        assertEquals(drone, restarted.droneFor("M-1").orElseThrow());
        assertEquals(Map.of("M-1", "SLOT-M-1"), restartedAir.slots());
    }

    @Test
    void emptyTextMeansNothingSaved() {
        assertNull(SimulationStateCodec.decode(""));
        assertNull(SimulationStateCodec.decode(null));
    }

    @Test
    void corruptTextIsRejectedClearly() {
        assertThrows(IllegalArgumentException.class, () -> SimulationStateCodec.decode("something else\n"));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationStateCodec.decode(SimulationStateCodec.HEADER + "\nD|only|three\n"));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationStateCodec.decode(SimulationStateCodec.HEADER + "\nX|unknown\n"));
    }

    @Test
    void unknownDroneIdsInSavedStateAreIgnored() {
        DroneFleet fleet = new DroneFleet(TestData.BASE, DroneFleet.demoSpecs());
        fleet.restore(List.of(new DroneSnapshot("DR-99", "Ghost", DroneStatus.IN_FLIGHT, 50, 0, TestData.BASE, "M-X")));
        assertEquals(3, fleet.list().size());
        assertEquals(true, fleet.droneFor("M-X").isEmpty());
    }
}
