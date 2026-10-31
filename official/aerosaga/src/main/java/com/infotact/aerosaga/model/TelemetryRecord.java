package com.infotact.aerosaga.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Historical flight telemetry persistence record for compliance, analysis, and replay.
 */
@Entity
@Table(name = "telemetry_records")
public class TelemetryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private String missionId;

    @Column(name = "drone_id", nullable = false)
    private String droneId;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "altitude_meters", nullable = false)
    private double altitudeMeters;

    @Column(name = "speed_mps", nullable = false)
    private double speedMps;

    @Column(name = "battery_percentage", nullable = false)
    private double batteryPercentage;

    @Column(name = "event_note")
    private String eventNote;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public TelemetryRecord() {
    }

    public TelemetryRecord(String missionId, String droneId, double latitude, double longitude,
                           double altitudeMeters, double speedMps, double batteryPercentage, String eventNote) {
        this.missionId = missionId;
        this.droneId = droneId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.speedMps = speedMps;
        this.batteryPercentage = batteryPercentage;
        this.eventNote = eventNote;
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public double getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(double batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public String getEventNote() {
        return eventNote;
    }

    public void setEventNote(String eventNote) {
        this.eventNote = eventNote;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
