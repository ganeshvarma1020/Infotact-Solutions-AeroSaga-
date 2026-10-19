package com.infotact.aerosaga.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "missions")
public class Mission {

    public enum Status {
        CREATED, SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "drone_id")
    private Drone drone;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "destination_lat", nullable = false)
    private Double destinationLat;

    @Column(name = "destination_lng", nullable = false)
    private Double destinationLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.CREATED;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Mission() {}

    public Mission(Drone drone, String name, Double destinationLat, Double destinationLng, Status status, LocalDateTime scheduledTime) {
        this.drone = drone;
        this.name = name;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.status = status;
        this.scheduledTime = scheduledTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Drone getDrone() { return drone; }
    public void setDrone(Drone drone) { this.drone = drone; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }

    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
