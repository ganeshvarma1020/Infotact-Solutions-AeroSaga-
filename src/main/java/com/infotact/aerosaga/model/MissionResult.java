package com.infotact.aerosaga.model;

import java.io.Serializable;

/**
 * Final execution summary and outcome returned by the Temporal workflow.
 */
public class MissionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String missionId;
    private String droneId;
    private MissionStatus status;
    private boolean success;
    private String message;
    private int waypointsCompleted;
    private double distanceCoveredKm;
    private double batteryRemainingPercent;
    private long durationSeconds;
    private boolean sagaCompensated;

    public MissionResult() {
    }

    public MissionResult(String missionId, String droneId, MissionStatus status, boolean success,
                         String message, int waypointsCompleted, double distanceCoveredKm,
                         double batteryRemainingPercent, long durationSeconds, boolean sagaCompensated) {
        this.missionId = missionId;
        this.droneId = droneId;
        this.status = status;
        this.success = success;
        this.message = message;
        this.waypointsCompleted = waypointsCompleted;
        this.distanceCoveredKm = distanceCoveredKm;
        this.batteryRemainingPercent = batteryRemainingPercent;
        this.durationSeconds = durationSeconds;
        this.sagaCompensated = sagaCompensated;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public MissionStatus getStatus() {
        return status;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getWaypointsCompleted() {
        return waypointsCompleted;
    }

    public void setWaypointsCompleted(int waypointsCompleted) {
        this.waypointsCompleted = waypointsCompleted;
    }

    public double getDistanceCoveredKm() {
        return distanceCoveredKm;
    }

    public void setDistanceCoveredKm(double distanceCoveredKm) {
        this.distanceCoveredKm = distanceCoveredKm;
    }

    public double getBatteryRemainingPercent() {
        return batteryRemainingPercent;
    }

    public void setBatteryRemainingPercent(double batteryRemainingPercent) {
        this.batteryRemainingPercent = batteryRemainingPercent;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isSagaCompensated() {
        return sagaCompensated;
    }

    public void setSagaCompensated(boolean sagaCompensated) {
        this.sagaCompensated = sagaCompensated;
    }
}
