package com.infotact.aerosaga.workflow;

/** Final outcome returned when the workflow ends. */
public record MissionResult(
        String missionId,
        MissionPhase outcome,
        String droneId,
        int legsCompleted,
        double distanceFlownKm,
        String failureCode,
        String failureReason) {
}
