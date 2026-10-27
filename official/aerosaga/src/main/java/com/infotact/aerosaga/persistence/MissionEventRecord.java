package com.infotact.aerosaga.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per step in a mission's history (audit log). Never updated, only inserted. */
@Entity
@Table(name = "mission_event")
public class MissionEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private String missionId;

    @Column(nullable = false)
    private String phase;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected MissionEventRecord() {
        // for JPA
    }

    public MissionEventRecord(String missionId, String phase, String message, Instant occurredAt) {
        this.missionId = missionId;
        this.phase = phase;
        this.message = MissionRecord.truncate(message);
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public String getMissionId() { return missionId; }
    public String getPhase() { return phase; }
    public String getMessage() { return message; }
    public Instant getOccurredAt() { return occurredAt; }
}
