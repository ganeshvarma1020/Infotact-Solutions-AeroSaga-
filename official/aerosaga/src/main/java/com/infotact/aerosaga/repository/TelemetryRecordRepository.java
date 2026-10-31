package com.infotact.aerosaga.repository;

import com.infotact.aerosaga.model.TelemetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, Long> {
    List<TelemetryRecord> findByMissionIdOrderByRecordedAtAsc(String missionId);
    List<TelemetryRecord> findTop50ByMissionIdOrderByRecordedAtDesc(String missionId);
}
