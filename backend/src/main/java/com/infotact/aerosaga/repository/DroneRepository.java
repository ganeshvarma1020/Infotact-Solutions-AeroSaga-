package com.infotact.aerosaga.repository;

import com.infotact.aerosaga.model.Drone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DroneRepository extends JpaRepository<Drone, Long> {
    List<Drone> findByStatus(Drone.Status status);
}
