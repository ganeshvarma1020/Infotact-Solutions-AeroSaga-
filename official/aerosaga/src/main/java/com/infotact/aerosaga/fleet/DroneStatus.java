package com.infotact.aerosaga.fleet;

public enum DroneStatus {
    /** On the pad, ready for a mission. */
    AVAILABLE,
    /** Booked for a mission, still on the ground. */
    ASSIGNED,
    /** Flying the mission route. */
    IN_FLIGHT,
    /** Flying back to base (normal end or emergency return). */
    RETURNING
}
