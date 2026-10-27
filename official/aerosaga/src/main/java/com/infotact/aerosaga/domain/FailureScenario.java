package com.infotact.aerosaga.domain;

/**
 * Faults the simulator can inject, so the saga rollback can be demonstrated on demand.
 * NONE means a normal mission.
 */
public enum FailureScenario {
    /** Normal mission. */
    NONE,
    /** Battery cell fault mid-flight: charge drops sharply, mission must abort and return home. */
    LOW_BATTERY,
    /** A temporary no-fly zone appears over a waypoint: drone must not enter, mission aborts. */
    GEOFENCE_BREACH,
    /** Telemetry link drops for the first attempts of a leg, then recovers. Shows automatic retries. */
    FLAKY_TELEMETRY
}
