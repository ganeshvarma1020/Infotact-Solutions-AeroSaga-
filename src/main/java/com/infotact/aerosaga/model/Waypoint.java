package com.infotact.aerosaga.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Geographic waypoint coordinate defining a node along the flight trajectory.
 */
public class Waypoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private double latitude;
    private double longitude;
    private double altitudeMeters;
    private double speedMps;
    private String label;
    private boolean reached;

    public Waypoint() {
    }

    public Waypoint(double latitude, double longitude, double altitudeMeters, double speedMps, String label) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.speedMps = speedMps;
        this.label = label;
        this.reached = false;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(double altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    public double getSpeedMps() {
        return speedMps;
    }

    public void setSpeedMps(double speedMps) {
        this.speedMps = speedMps;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isReached() {
        return reached;
    }

    public void setReached(boolean reached) {
        this.reached = reached;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Waypoint)) return false;
        Waypoint waypoint = (Waypoint) o;
        return Double.compare(waypoint.latitude, latitude) == 0 &&
               Double.compare(waypoint.longitude, longitude) == 0 &&
               Double.compare(waypoint.altitudeMeters, altitudeMeters) == 0 &&
               Objects.equals(label, waypoint.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, altitudeMeters, label);
    }

    @Override
    public String toString() {
        return "Waypoint{" +
                "label='" + label + '\'' +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", alt=" + altitudeMeters + "m" +
                ", reached=" + reached +
                '}';
    }
}
