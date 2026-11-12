package com.infotact.aerosaga.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity tracking full mission history and audit records.
 */
@Entity
@Table(name = "drone_missions")
public class DroneMission {

    @Id
    @Column(name = "mission_id", length = 64, nullable = false)
    private String missionId;

    @Column(name = "drone_id", length = 64, nullable = false)
    private String droneId;

    @Column(name = "workflow_id", length = 128, nullable = false)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MissionStatus status;

    @Column(name = "start_latitude")
    private double startLatitude;

    @Column(name = "start_longitude")
    private double startLongitude;

    @Column(name = "target_latitude")
    private double targetLatitude;

    @Column(name = "target_longitude")
    private double targetLongitude;

    @Column(name = "mission_summary", length = 512)
    private String missionSummary;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "saga_compensated", nullable = false)
    private boolean sagaCompensated;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "distance_covered_km")
    private double distanceCoveredKm;

    public DroneMission() {
    }

    public DroneMission(String missionId, String droneId, String workflowId, MissionType missionType) {
        this.missionId = missionId;
        this.droneId = droneId;
        this.workflowId = workflowId;
        this.missionType = missionType;
        this.status = MissionStatus.PENDING;
        this.sagaCompensated = false;
        this.startTime = LocalDateTime.now();
        this.distanceCoveredKm = 0.0;
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

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public void setMissionType(MissionType missionType) {
        this.missionType = missionType;
    }

    public MissionStatus getStatus() {
        return status;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

    public double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(double startLongitude) {
        this.startLongitude = startLongitude;
    }

    public double getTargetLatitude() {
        return targetLatitude;
    }

    public void setTargetLatitude(double targetLatitude) {
        this.targetLatitude = targetLatitude;
    }

    public double getTargetLongitude() {
        return targetLongitude;
    }

    public void setTargetLongitude(double targetLongitude) {
        this.targetLongitude = targetLongitude;
    }

    public String getMissionSummary() {
        return missionSummary;
    }

    public void setMissionSummary(String missionSummary) {
        this.missionSummary = missionSummary;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean isSagaCompensated() {
        return sagaCompensated;
    }

    public void setSagaCompensated(boolean sagaCompensated) {
        this.sagaCompensated = sagaCompensated;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public double getDistanceCoveredKm() {
        return distanceCoveredKm;
    }

    public void setDistanceCoveredKm(double distanceCoveredKm) {
        this.distanceCoveredKm = distanceCoveredKm;
    }
}
