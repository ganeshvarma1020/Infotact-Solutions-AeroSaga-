package com.infotact.aerosaga.repository;

import com.infotact.aerosaga.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    List<Telemetry> findByMissionIdOrderByTimestampAsc(Long missionId);
    List<Telemetry> findByDroneIdOrderByTimestampDesc(Long droneId);
}
