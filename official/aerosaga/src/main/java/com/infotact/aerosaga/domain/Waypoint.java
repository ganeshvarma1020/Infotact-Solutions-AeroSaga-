package com.infotact.aerosaga.domain;

/** A named stop on a mission route. */
public record Waypoint(String name, GeoPoint position) {

    public Waypoint {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Waypoint name is required");
        }
        if (position == null) {
            throw new IllegalArgumentException("Waypoint position is required");
        }
    }
}
