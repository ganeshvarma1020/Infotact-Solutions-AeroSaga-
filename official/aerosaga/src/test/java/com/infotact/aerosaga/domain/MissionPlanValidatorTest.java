package com.infotact.aerosaga.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.infotact.aerosaga.TestData;

class MissionPlanValidatorTest {

    private final MissionPlanValidator validator = new MissionPlanValidator(TestData.LIMITS);

    private static boolean has(ValidationResult r, String fragment) {
        return r.errors().stream().anyMatch(e -> e.contains(fragment));
    }

    private static MissionRequest withWaypoints(List<Waypoint> wps) {
        return new MissionRequest("M-1", "op", TestData.BASE, wps, 1.0, FailureScenario.NONE);
    }

    @Test
    void acceptsNormalMission() {
        ValidationResult r = validator.validate(TestData.mission("M-1"));
        assertTrue(r.valid(), () -> "unexpected errors: " + r.errors());
    }

    @Test
    void rejectsMissingFields() {
        ValidationResult r = validator.validate(
                new MissionRequest(" ", null, TestData.BASE, List.of(), 1.0, null));
        assertFalse(r.valid());
        assertTrue(has(r, "missionId is required"));
        assertTrue(has(r, "operator is required"));
        assertTrue(has(r, "at least 1 waypoint"));
    }

    @Test
    void rejectsNullRequest() {
        assertFalse(validator.validate(null).valid());
    }

    @Test
    void rejectsPayloadOutOfRange() {
        assertTrue(has(validator.validate(TestData.mission("M-1", 9.0, FailureScenario.NONE)), "exceeds fleet max"));
        assertTrue(has(validator.validate(TestData.mission("M-1", -1.0, FailureScenario.NONE)), "cannot be negative"));
    }

    @Test
    void rejectsTooManyWaypoints() {
        List<Waypoint> many = Collections.nCopies(11, TestData.BOMMASANDRA);
        assertTrue(has(validator.validate(withWaypoints(many)), "too many waypoints"));
    }

    @Test
    void rejectsWaypointOutsideOperatingArea() {
        Waypoint mysuru = new Waypoint("Mysuru", new GeoPoint(12.2958, 76.6394));
        assertTrue(has(validator.validate(withWaypoints(List.of(mysuru))), "from the operating center"));
    }

    @Test
    void rejectsWaypointInsideNoFlyZone() {
        Waypoint hal = new Waypoint("HAL", new GeoPoint(12.9500, 77.6682));
        assertTrue(has(validator.validate(withWaypoints(List.of(hal))), "inside no-fly zone 'HAL Airport'"));
    }

    @Test
    void rejectsLegThatCrossesNoFlyZone() {
        // North of HAL airport: endpoint is outside the zone but the straight line passes over it
        Waypoint north = new Waypoint("North Point", new GeoPoint(12.9900, 77.6700));
        ValidationResult r = validator.validate(withWaypoints(List.of(north)));
        assertTrue(has(r, "crosses no-fly zone 'HAL Airport'"), () -> "errors: " + r.errors());
    }

    @Test
    void rejectsRouteLongerThanLimit() {
        // Out and back across the area: about 2 x 22 km = 44 km > 40 km limit
        Waypoint far = new Waypoint("Far East", new GeoPoint(12.8456, 77.8630));
        ValidationResult r = validator.validate(withWaypoints(List.of(far)));
        assertTrue(has(r, "exceeds max"), () -> "errors: " + r.errors());
    }

    @Test
    void rejectsDuplicateConsecutiveWaypoint() {
        ValidationResult r = validator.validate(withWaypoints(List.of(TestData.BOMMASANDRA, TestData.BOMMASANDRA)));
        assertTrue(has(r, "duplicates the previous point"));
    }

    @Test
    void reportsEveryProblemNotJustTheFirst() {
        ValidationResult r = validator.validate(new MissionRequest("", "", TestData.BASE, List.of(), 99, null));
        assertEquals(4, r.errors().size(), () -> "errors: " + r.errors());
    }
}
