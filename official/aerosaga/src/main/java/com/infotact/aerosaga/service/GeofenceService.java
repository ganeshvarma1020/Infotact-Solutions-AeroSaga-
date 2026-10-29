package com.infotact.aerosaga.service;

import com.infotact.aerosaga.model.Waypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service enforcing FAA/EASA compliant geofenced airspace restrictions and dynamic NFZ (No-Fly Zones).
 */
@Service
public class GeofenceService {

    private static final Logger logger = LoggerFactory.getLogger(GeofenceService.class);

    public static class NoFlyZone {
        private final String name;
        private final double centerLat;
        private final double centerLon;
        private final double radiusKm;

        public NoFlyZone(String name, double centerLat, double centerLon, double radiusKm) {
            this.name = name;
            this.centerLat = centerLat;
            this.centerLon = centerLon;
            this.radiusKm = radiusKm;
        }

        public String getName() {
            return name;
        }

        public double getCenterLat() {
            return centerLat;
        }

        public double getCenterLon() {
            return centerLon;
        }

        public double getRadiusKm() {
            return radiusKm;
        }
    }

    private final List<NoFlyZone> restrictedZones = new ArrayList<>();

    public GeofenceService() {
        // Sample restricted airspaces around test metropolitan area (e.g., Regional Airport & Government Complex)
        restrictedZones.add(new NoFlyZone("International Airport Airspace Class B", 12.9850, 80.1700, 2.5));
        restrictedZones.add(new NoFlyZone("Naval Defense Restricted Zone NFZ-04", 13.0100, 80.2800, 1.8));
    }

    public List<NoFlyZone> getRestrictedZones() {
        return restrictedZones;
    }

    public boolean isWaypointInsideNoFlyZone(Waypoint waypoint) {
        for (NoFlyZone zone : restrictedZones) {
            double distanceKm = calculateHaversineDistanceKm(
                    zone.getCenterLat(), zone.getCenterLon(),
                    waypoint.getLatitude(), waypoint.getLongitude()
            );
            if (distanceKm <= zone.getRadiusKm()) {
                logger.warn("Waypoint {} violates restricted airspace '{}' (Distance: {} km <= Radius: {} km)",
                        waypoint.getLabel(), zone.getName(), Math.round(distanceKm * 100.0) / 100.0, zone.getRadiusKm());
                return true;
            }
        }
        return false;
    }

    public static double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
