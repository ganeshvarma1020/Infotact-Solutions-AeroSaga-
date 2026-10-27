package com.infotact.aerosaga.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per mission: what was asked, and how it ended. */
@Entity
@Table(name = "mission")
public class MissionRecord {

    @Id
    private String id;

    @Column(nullable = false)
    private String operator;

    @Column(name = "payload_kg", nullable = false)
    private double payloadKg;

    @Column(name = "waypoint_count", nullable = false)
    private int waypointCount;

    @Column(name = "route_km", nullable = false)
    private double routeKm;

    @Column(nullable = false)
    private String scenario;

    @Column(nullable = false)
    private String phase;

    @Column(name = "drone_id")
    private String droneId;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected MissionRecord() {
        // for JPA
    }

    public MissionRecord(String id, String operator, double payloadKg, int waypointCount, double routeKm,
                         String scenario, String phase, Instant now) {
        this.id = id;
        this.operator = operator;
        this.payloadKg = payloadKg;
        this.waypointCount = waypointCount;
        this.routeKm = routeKm;
        this.scenario = scenario;
        this.phase = phase;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String phase, String droneId, Instant at) {
        this.phase = phase;
        if (droneId != null) {
            this.droneId = droneId;
        }
        this.updatedAt = at;
    }

    public void finish(String phase, String droneId, String failureCode, String failureReason, Instant at) {
        update(phase, droneId, at);
        this.failureCode = failureCode;
        this.failureReason = truncate(failureReason);
        this.finishedAt = at;
    }

    static String truncate(String s) {
        return s == null || s.length() <= 1000 ? s : s.substring(0, 997) + "...";
    }

    public String getId() { return id; }
    public String getOperator() { return operator; }
    public double getPayloadKg() { return payloadKg; }
    public int getWaypointCount() { return waypointCount; }
    public double getRouteKm() { return routeKm; }
    public String getScenario() { return scenario; }
    public String getPhase() { return phase; }
    public String getDroneId() { return droneId; }
    public String getFailureCode() { return failureCode; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
