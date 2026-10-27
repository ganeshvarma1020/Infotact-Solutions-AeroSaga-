package com.infotact.aerosaga.airspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.MissionException;
import com.infotact.aerosaga.domain.NoFlyZone;

class AirspaceRegistryTest {

    @Test
    void reserveIsIdempotent() {
        AirspaceRegistry a = new AirspaceRegistry(1);
        String s1 = a.reserve("M-1");
        String s2 = a.reserve("M-1");
        assertEquals(s1, s2);
        assertEquals(1, a.inUse());
    }

    @Test
    void enforcesCapacity() {
        AirspaceRegistry a = new AirspaceRegistry(2);
        a.reserve("M-1");
        a.reserve("M-2");
        MissionException e = assertThrows(MissionException.class, () -> a.reserve("M-3"));
        assertEquals(MissionException.Code.AIRSPACE_FULL, e.code());
    }

    @Test
    void releaseFreesSlotAndIsSafeToRepeat() {
        AirspaceRegistry a = new AirspaceRegistry(1);
        a.reserve("M-1");
        a.release("M-1");
        a.release("M-1");
        assertFalse(a.holds("M-1"));
        a.reserve("M-2");
        assertTrue(a.holds("M-2"));
    }

    @Test
    void temporaryZonesCanBeAddedReplacedAndCleared() {
        AirspaceRegistry a = new AirspaceRegistry(1);
        a.addTemporaryZone(new NoFlyZone("TFR", new GeoPoint(12.9, 77.6), 1));
        a.addTemporaryZone(new NoFlyZone("TFR", new GeoPoint(12.9, 77.6), 2));
        assertEquals(1, a.temporaryZones().size());
        assertEquals(2.0, a.temporaryZones().get(0).radiusKm(), 1e-9);
        a.clearTemporaryZone("TFR");
        assertTrue(a.temporaryZones().isEmpty());
    }

    @Test
    void rejectsZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new AirspaceRegistry(0));
    }
}
