package com.infotact.aerosaga.repository;

import com.infotact.aerosaga.model.DroneMission;
import com.infotact.aerosaga.model.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DroneMissionRepository extends JpaRepository<DroneMission, String> {
    List<DroneMission> findByDroneId(String droneId);
    List<DroneMission> findByStatus(MissionStatus status);
    Optional<DroneMission> findByWorkflowId(String workflowId);
}
