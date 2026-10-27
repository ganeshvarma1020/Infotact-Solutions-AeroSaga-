package com.infotact.aerosaga.domain;

/** A circular area drones must never enter (airport, military site, temporary restriction). */
public record NoFlyZone(String name, GeoPoint center, double radiusKm) {

    public NoFlyZone {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("No-fly zone name is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("No-fly zone center is required");
        }
        if (!(radiusKm > 0)) {
            throw new IllegalArgumentException("No-fly zone radius must be positive");
        }
    }

    public boolean contains(GeoPoint point) {
        return center.distanceKmTo(point) <= radiusKm;
    }

    /**
     * True if the straight line from -> to passes through this zone.
     * Samples the line every ~100 m. Good enough for routes of tens of km
     * and far simpler to verify than exact spherical geometry.
     */
    public boolean crossedBy(GeoPoint from, GeoPoint to) {
        int steps = Math.max(1, (int) Math.ceil(from.distanceKmTo(to) / 0.1));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            GeoPoint p = new GeoPoint(
                    from.lat() + (to.lat() - from.lat()) * t,
                    from.lon() + (to.lon() - from.lon()) * t);
            if (contains(p)) {
                return true;
            }
        }
        return false;
    }
}
