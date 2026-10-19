package com.infotact.aerosaga.repository;

import com.infotact.aerosaga.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByStatus(Mission.Status status);
    List<Mission> findByDroneId(Long droneId);
}
