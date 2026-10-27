package com.infotact.aerosaga.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.infotact.aerosaga.TestData;

class GeoAndRouteTest {

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        // Independent check: arc length = R * angle = 6371.0088 * pi / 180 = 111.195 km
        double km = new GeoPoint(0, 0).distanceKmTo(new GeoPoint(1, 0));
        assertEquals(111.195, km, 0.001);
    }

    @Test
    void distanceIsSymmetricAndZeroToSelf() {
        GeoPoint a = TestData.BASE;
        GeoPoint b = TestData.BOMMASANDRA.position();
        assertEquals(a.distanceKmTo(b), b.distanceKmTo(a), 1e-9);
        assertEquals(0.0, a.distanceKmTo(a), 1e-9);
    }

    @Test
    void rejectsImpossibleCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(91, 0));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(0, -181));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(Double.NaN, 0));
    }

    @Test
    void routeHasOneLegPerWaypointPlusReturn() {
        RoutePlan route = TestData.mission("M-1").route();
        assertEquals(3, route.legs().size());
        assertEquals(2, route.outboundLegs().size());
        assertEquals(RoutePlan.BASE_NAME, route.returnLeg().toName());
        assertEquals(TestData.BASE, route.returnLeg().to());
        double sum = route.legs().stream().mapToDouble(RoutePlan.Leg::distanceKm).sum();
        assertEquals(sum, route.totalKm(), 1e-9);
    }

    @Test
    void emptyRouteHasNoLegs() {
        RoutePlan route = RoutePlan.of(TestData.BASE, List.of());
        assertTrue(route.legs().isEmpty());
        assertThrows(IllegalStateException.class, route::returnLeg);
    }

    @Test
    void noFlyZoneDetectsPointsAndCrossings() {
        NoFlyZone zone = new NoFlyZone("Z", new GeoPoint(12.90, 77.66), 1.0);
        assertTrue(zone.contains(new GeoPoint(12.90, 77.66)));
        assertFalse(zone.contains(new GeoPoint(12.95, 77.66)));
        // Straight line from south to north passes through the center
        assertTrue(zone.crossedBy(new GeoPoint(12.85, 77.66), new GeoPoint(12.95, 77.66)));
        // Line well to the east misses it
        assertFalse(zone.crossedBy(new GeoPoint(12.85, 77.70), new GeoPoint(12.95, 77.70)));
    }
}
