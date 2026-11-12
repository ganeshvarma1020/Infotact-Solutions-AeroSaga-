package com.infotact.aerosaga.model;

/**
 * Enum representing the current life-cycle state of a Drone Mission.
 */
public enum MissionStatus {
    PENDING,
    PREFLIGHT_INSPECTION,
    TAKEOFF,
    EN_ROUTE,
    DELIVERING_PAYLOAD,
    SURVEILLANCE_ACTIVE,
    RETURNING_TO_BASE,
    LANDING,
    COMPLETED,
    ABORTED_SAFE_LANDING,
    FAILED
}
