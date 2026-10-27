package com.infotact.aerosaga;

import java.util.List;

import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.MissionRequest;
import com.infotact.aerosaga.domain.OperatingLimits;
import com.infotact.aerosaga.domain.Waypoint;

/** Shared, clearly synthetic test data. Coordinates are around Electronic City, Bengaluru. */
public final class TestData {

    public static final OperatingLimits LIMITS = OperatingLimits.bengaluruDemo();
    public static final GeoPoint BASE = LIMITS.operatingCenter();
    public static final Waypoint BOMMASANDRA = new Waypoint("Bommasandra", new GeoPoint(12.8169, 77.6950));
    public static final Waypoint HOSUR_ROAD = new Waypoint("Hosur Road Hub", new GeoPoint(12.8700, 77.6400));

    private TestData() {
    }

    public static MissionRequest mission(String id, double payloadKg, FailureScenario scenario) {
        return new MissionRequest(id, "test-operator", BASE, List.of(BOMMASANDRA, HOSUR_ROAD), payloadKg, scenario);
    }

    public static MissionRequest mission(String id) {
        return mission(id, 1.0, FailureScenario.NONE);
    }
}
