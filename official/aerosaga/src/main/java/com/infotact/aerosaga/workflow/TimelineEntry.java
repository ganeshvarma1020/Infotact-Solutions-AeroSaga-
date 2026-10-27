package com.infotact.aerosaga.workflow;

/** One human-readable line in a mission's history. Time comes from Temporal's deterministic clock. */
public record TimelineEntry(long atEpochMillis, MissionPhase phase, String message) {
}
