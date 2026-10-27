package com.infotact.aerosaga.domain;

/**
 * A point on Earth (latitude, longitude in degrees).
 * Immutable. Rejects impossible coordinates at creation time.
 */
public record GeoPoint(double lat, double lon) {

    /** Mean Earth radius in km (IUGG value). */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    public GeoPoint {
        if (Double.isNaN(lat) || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, got " + lat);
        }
        if (Double.isNaN(lon) || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180, got " + lon);
        }
    }

    /** Great-circle distance using the haversine formula. */
    public double distanceKmTo(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - lat);
        double dLon = Math.toRadians(other.lon - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
