package com.infotact.aerosaga.model;

import jakarta.persistence.*;

@Entity
@Table(name = "drones")
public class Drone {

    public enum Status {
        AVAILABLE, IN_MISSION, MAINTENANCE, OFFLINE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "battery_level", nullable = false)
    private Double batteryLevel = 100.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.AVAILABLE;

    @Column(name = "current_lat", nullable = false)
    private Double currentLat = 0.0;

    @Column(name = "current_lng", nullable = false)
    private Double currentLng = 0.0;

    public Drone() {}

    public Drone(String name, String model, Double batteryLevel, Status status, Double currentLat, Double currentLng) {
        this.name = name;
        this.model = model;
        this.batteryLevel = batteryLevel;
        this.status = status;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Double batteryLevel) { this.batteryLevel = batteryLevel; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Double getCurrentLat() { return currentLat; }
    public void setCurrentLat(Double currentLat) { this.currentLat = currentLat; }

    public Double getCurrentLng() { return currentLng; }
    public void setCurrentLng(Double currentLng) { this.currentLng = currentLng; }
}
