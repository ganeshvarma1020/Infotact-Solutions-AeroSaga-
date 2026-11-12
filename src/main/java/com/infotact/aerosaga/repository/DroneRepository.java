package com.infotact.aerosaga.repository;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.DroneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DroneRepository extends JpaRepository<Drone, String> {
    List<Drone> findByStatus(DroneStatus status);
}
