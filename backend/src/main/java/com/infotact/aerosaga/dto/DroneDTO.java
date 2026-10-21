package com.infotact.aerosaga.dto;

import com.infotact.aerosaga.model.Drone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DroneDTO {

    private Long id;

    @NotBlank(message = "Drone name cannot be blank")
    private String name;

    @NotBlank(message = "Drone model cannot be blank")
    private String model;

    @NotNull(message = "Battery level is required")
    private Double batteryLevel;

    private Drone.Status status = Drone.Status.AVAILABLE;

    @NotNull(message = "Latitude is required")
    private Double currentLat;

    @NotNull(message = "Longitude is required")
    private Double currentLng;

    public DroneDTO() {}

    public DroneDTO(Long id, String name, String model, Double batteryLevel, Drone.Status status, Double currentLat, Double currentLng) {
        this.id = id;
        this.name = name;
        this.model = model;
        this.batteryLevel = batteryLevel;
        this.status = status;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
    }

    public static DroneDTO fromEntity(Drone drone) {
        return new DroneDTO(
                drone.getId(),
                drone.getName(),
                drone.getModel(),
                drone.getBatteryLevel(),
                drone.getStatus(),
                drone.getCurrentLat(),
                drone.getCurrentLng()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Double batteryLevel) { this.batteryLevel = batteryLevel; }

    public Drone.Status getStatus() { return status; }
    public void setStatus(Drone.Status status) { this.status = status; }

    public Double getCurrentLat() { return currentLat; }
    public void setCurrentLat(Double currentLat) { this.currentLat = currentLat; }

    public Double getCurrentLng() { return currentLng; }
    public void setCurrentLng(Double currentLng) { this.currentLng = currentLng; }
}
