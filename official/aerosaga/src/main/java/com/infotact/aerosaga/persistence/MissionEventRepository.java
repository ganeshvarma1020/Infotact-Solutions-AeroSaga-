package com.infotact.aerosaga.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionEventRepository extends JpaRepository<MissionEventRecord, Long> {

    List<MissionEventRecord> findByMissionIdOrderByOccurredAtAscIdAsc(String missionId);
}
