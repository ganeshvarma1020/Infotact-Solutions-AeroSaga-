package com.infotact.aerosaga.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a drone unit registered within the AeroSaga fleet.
 */
@Entity
@Table(name = "drones")
public class Drone {

    @Id
    @Column(name = "drone_id", length = 64, nullable = false)
    private String id;

    @Column(name = "model_name", nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DroneStatus status;

    @Column(name = "battery_percentage", nullable = false)
    private double batteryPercentage;

    @Column(name = "max_payload_kg", nullable = false)
    private double maxPayloadKg;

    @Column(name = "current_payload_kg", nullable = false)
    private double currentPayloadKg;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "altitude_meters", nullable = false)
    private double altitudeMeters;

    @Column(name = "total_flight_hours", nullable = false)
    private double totalFlightHours;

    @Column(name = "last_maintenance")
    private LocalDateTime lastMaintenance;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    public Drone() {
    }

    public Drone(String id, String model, double maxPayloadKg, double latitude, double longitude) {
        this.id = id;
        this.model = model;
        this.status = DroneStatus.IDLE;
        this.batteryPercentage = 100.0;
        this.maxPayloadKg = maxPayloadKg;
        this.currentPayloadKg = 0.0;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = 0.0;
        this.totalFlightHours = 0.0;
        this.lastMaintenance = LocalDateTime.now();
        this.registeredAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public DroneStatus getStatus() {
        return status;
    }

    public void setStatus(DroneStatus status) {
        this.status = status;
    }

    public double getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(double batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public double getMaxPayloadKg() {
        return maxPayloadKg;
    }

    public void setMaxPayloadKg(double maxPayloadKg) {
        this.maxPayloadKg = maxPayloadKg;
    }

    public double getCurrentPayloadKg() {
        return currentPayloadKg;
    }

    public void setCurrentPayloadKg(double currentPayloadKg) {
        this.currentPayloadKg = currentPayloadKg;
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

    public double getTotalFlightHours() {
        return totalFlightHours;
    }

    public void setTotalFlightHours(double totalFlightHours) {
        this.totalFlightHours = totalFlightHours;
    }

    public LocalDateTime getLastMaintenance() {
        return lastMaintenance;
    }

    public void setLastMaintenance(LocalDateTime lastMaintenance) {
        this.lastMaintenance = lastMaintenance;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}
