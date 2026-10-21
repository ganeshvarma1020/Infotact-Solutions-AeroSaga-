package com.infotact.aerosaga.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class MissionCreateDTO {

    private Long droneId; // Optional: system can auto-assign available drone if null

    @NotBlank(message = "Mission name cannot be blank")
    private String name;

    @NotNull(message = "Destination latitude is required")
    private Double destinationLat;

    @NotNull(message = "Destination longitude is required")
    private Double destinationLng;

    private LocalDateTime scheduledTime;

    public MissionCreateDTO() {}

    public MissionCreateDTO(Long droneId, String name, Double destinationLat, Double destinationLng, LocalDateTime scheduledTime) {
        this.droneId = droneId;
        this.name = name;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.scheduledTime = scheduledTime;
    }

    public Long getDroneId() { return droneId; }
    public void setDroneId(Long droneId) { this.droneId = droneId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }

    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
}
