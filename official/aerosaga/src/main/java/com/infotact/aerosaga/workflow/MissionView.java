package com.infotact.aerosaga.workflow;

import java.util.List;

import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.Waypoint;

/** Live picture of a mission, returned by the workflow query. Read-only copy. */
public record MissionView(
        String missionId,
        MissionPhase phase,
        FailureScenario scenario,
        double payloadKg,
        String droneId,
        String slotId,
        int legsCompleted,
        int legsTotal,
        String nextStop,
        GeoPoint position,
        Double batteryPct,
        double distanceFlownKm,
        boolean abortRequested,
        String failureCode,
        String failureReason,
        List<Waypoint> route,
        List<TimelineEntry> timeline) {
}
