package com.infotact.aerosaga.domain;

import java.util.List;

/**
 * What an operator asks for: fly from base through the waypoints carrying a payload, then return.
 * Field checks that need business rules live in {@link MissionPlanValidator}, not here,
 * so that a bad request can still be created and reported back with clear errors.
 */
public record MissionRequest(
        String missionId,
        String operator,
        GeoPoint base,
        List<Waypoint> waypoints,
        double payloadKg,
        FailureScenario failureScenario) {

    public MissionRequest {
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
        failureScenario = failureScenario == null ? FailureScenario.NONE : failureScenario;
    }

    /** The full route this mission would fly, including the return leg to base. */
    public RoutePlan route() {
        return RoutePlan.of(base, waypoints);
    }
}
