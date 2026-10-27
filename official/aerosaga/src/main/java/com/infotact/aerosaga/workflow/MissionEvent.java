package com.infotact.aerosaga.workflow;

/** A step that happened, sent from the workflow to the journal (database). */
public record MissionEvent(String missionId, MissionPhase phase, String message, String droneId, long atEpochMillis) {
}
