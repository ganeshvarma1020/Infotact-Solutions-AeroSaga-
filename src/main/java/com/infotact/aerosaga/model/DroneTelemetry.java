package com.infotact.aerosaga.model;

import java.io.Serializable;

/**
 * Real-time operational snapshot of drone avionics, telemetry, and spatial position.
 */
public class DroneTelemetry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String droneId;
    private String missionId;
    private double latitude;
    private double longitude;
    private double altitudeMeters;
    private double speedMps;
    private double headingDegrees;
    private double batteryPercentage;
    private double batteryVoltage;
    private double motorTemperatureCelsius;
    private int satelliteCount;
    private double windSpeedKnots;
    private DroneStatus status;
    private String currentWaypoint;
    private long timestamp;

    public DroneTelemetry() {
        this.timestamp = System.currentTimeMillis();
    }

    public DroneTelemetry(String droneId, String missionId, double latitude, double longitude,
                          double altitudeMeters, double speedMps, double headingDegrees,
                          double batteryPercentage, double batteryVoltage, double motorTemperatureCelsius,
                          int satelliteCount, double windSpeedKnots, DroneStatus status,
                          String currentWaypoint) {
        this.droneId = droneId;
        this.missionId = missionId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.speedMps = speedMps;
        this.headingDegrees = headingDegrees;
        this.batteryPercentage = batteryPercentage;
        this.batteryVoltage = batteryVoltage;
        this.motorTemperatureCelsius = motorTemperatureCelsius;
        this.satelliteCount = satelliteCount;
        this.windSpeedKnots = windSpeedKnots;
        this.status = status;
        this.currentWaypoint = currentWaypoint;
        this.timestamp = System.currentTimeMillis();
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
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

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    public void setHeadingDegrees(double headingDegrees) {
        this.headingDegrees = headingDegrees;
    }

    public double getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(double batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public double getMotorTemperatureCelsius() {
        return motorTemperatureCelsius;
    }

    public void setMotorTemperatureCelsius(double motorTemperatureCelsius) {
        this.motorTemperatureCelsius = motorTemperatureCelsius;
    }

    public int getSatelliteCount() {
        return satelliteCount;
    }

    public void setSatelliteCount(int satelliteCount) {
        this.satelliteCount = satelliteCount;
    }

    public double getWindSpeedKnots() {
        return windSpeedKnots;
    }

    public void setWindSpeedKnots(double windSpeedKnots) {
        this.windSpeedKnots = windSpeedKnots;
    }

    public DroneStatus getStatus() {
        return status;
    }

    public void setStatus(DroneStatus status) {
        this.status = status;
    }

    public String getCurrentWaypoint() {
        return currentWaypoint;
    }

    public void setCurrentWaypoint(String currentWaypoint) {
        this.currentWaypoint = currentWaypoint;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
