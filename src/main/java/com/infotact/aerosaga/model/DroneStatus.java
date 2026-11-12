package com.infotact.aerosaga.model;

/**
 * Enum representing physical and operational state of a Drone asset.
 */
public enum DroneStatus {
    IDLE,
    PREFLIGHT,
    AIRBORNE,
    HOVERING,
    RETURNING_HOME,
    EMERGENCY_LANDING,
    MAINTENANCE_REQUIRED
}
