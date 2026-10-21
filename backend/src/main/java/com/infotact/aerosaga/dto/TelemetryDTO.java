package com.infotact.aerosaga.dto;

import com.infotact.aerosaga.model.Telemetry;
import java.time.LocalDateTime;

public class TelemetryDTO {

    private Long id;
    private Long missionId;
    private Long droneId;
    private Double batteryPercentage;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private LocalDateTime timestamp;

    public TelemetryDTO() {}

    public TelemetryDTO(Long id, Long missionId, Long droneId, Double batteryPercentage,
                        Double latitude, Double longitude, Double altitude, Double speed, LocalDateTime timestamp) {
        this.id = id;
        this.missionId = missionId;
        this.droneId = droneId;
        this.batteryPercentage = batteryPercentage;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.timestamp = timestamp;
    }

    public static TelemetryDTO fromEntity(Telemetry t) {
        return new TelemetryDTO(
                t.getId(),
                t.getMission() != null ? t.getMission().getId() : null,
                t.getDrone() != null ? t.getDrone().getId() : null,
                t.getBatteryPercentage(),
                t.getLatitude(),
                t.getLongitude(),
                t.getAltitude(),
                t.getSpeed(),
                t.getTimestamp()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMissionId() { return missionId; }
    public void setMissionId(Long missionId) { this.missionId = missionId; }

    public Long getDroneId() { return droneId; }
    public void setDroneId(Long droneId) { this.droneId = droneId; }

    public Double getBatteryPercentage() { return batteryPercentage; }
    public void setBatteryPercentage(Double batteryPercentage) { this.batteryPercentage = batteryPercentage; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
