package com.infotact.aerosaga.dto;

import com.infotact.aerosaga.model.Mission;
import java.time.LocalDateTime;

public class MissionDTO {

    private Long id;
    private Long droneId;
    private String droneName;
    private String name;
    private Double destinationLat;
    private Double destinationLng;
    private Mission.Status status;
    private LocalDateTime scheduledTime;
    private LocalDateTime createdAt;

    public MissionDTO() {}

    public MissionDTO(Long id, Long droneId, String droneName, String name, Double destinationLat, Double destinationLng,
                      Mission.Status status, LocalDateTime scheduledTime, LocalDateTime createdAt) {
        this.id = id;
        this.droneId = droneId;
        this.droneName = droneName;
        this.name = name;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.status = status;
        this.scheduledTime = scheduledTime;
        this.createdAt = createdAt;
    }

    public static MissionDTO fromEntity(Mission mission) {
        Long dId = mission.getDrone() != null ? mission.getDrone().getId() : null;
        String dName = mission.getDrone() != null ? mission.getDrone().getName() : null;
        return new MissionDTO(
                mission.getId(),
                dId,
                dName,
                mission.getName(),
                mission.getDestinationLat(),
                mission.getDestinationLng(),
                mission.getStatus(),
                mission.getScheduledTime(),
                mission.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDroneId() { return droneId; }
    public void setDroneId(Long droneId) { this.droneId = droneId; }

    public String getDroneName() { return droneName; }
    public void setDroneName(String droneName) { this.droneName = droneName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }

    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }

    public Mission.Status getStatus() { return status; }
    public void setStatus(Mission.Status status) { this.status = status; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
