package com.infotact.aerosaga.repository;

import com.infotact.aerosaga.model.MissionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionEventRepository extends JpaRepository<MissionEvent, Long> {
    List<MissionEvent> findByMissionIdOrderByTimestampAsc(Long missionId);
}
