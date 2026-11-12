package com.infotact.aerosaga.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Incoming client DTO requesting autonomous flight execution.
 */
public class MissionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String droneId;
    private MissionType missionType;
    private List<Waypoint> waypoints = new ArrayList<>();
    private double payloadWeightKg;
    private String priority = "NORMAL"; // NORMAL, URGENT, CRITICAL
    private boolean simulateSevereWeather = false;
    private boolean simulateMotorFailure = false;
    private boolean simulateBatteryDrain = false;

    public MissionRequest() {
    }

    public MissionRequest(String droneId, MissionType missionType, List<Waypoint> waypoints, double payloadWeightKg) {
        this.droneId = droneId;
        this.missionType = missionType;
        this.waypoints = waypoints;
        this.payloadWeightKg = payloadWeightKg;
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public void setMissionType(MissionType missionType) {
        this.missionType = missionType;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public double getPayloadWeightKg() {
        return payloadWeightKg;
    }

    public void setPayloadWeightKg(double payloadWeightKg) {
        this.payloadWeightKg = payloadWeightKg;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isSimulateSevereWeather() {
        return simulateSevereWeather;
    }

    public void setSimulateSevereWeather(boolean simulateSevereWeather) {
        this.simulateSevereWeather = simulateSevereWeather;
    }

    public boolean isSimulateMotorFailure() {
        return simulateMotorFailure;
    }

    public void setSimulateMotorFailure(boolean simulateMotorFailure) {
        this.simulateMotorFailure = simulateMotorFailure;
    }

    public boolean isSimulateBatteryDrain() {
        return simulateBatteryDrain;
    }

    public void setSimulateBatteryDrain(boolean simulateBatteryDrain) {
        this.simulateBatteryDrain = simulateBatteryDrain;
    }
}
