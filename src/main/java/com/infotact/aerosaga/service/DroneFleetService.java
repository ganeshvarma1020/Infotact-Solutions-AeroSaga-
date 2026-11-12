package com.infotact.aerosaga.service;

import com.infotact.aerosaga.exception.DroneNotFoundException;
import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.DroneStatus;
import com.infotact.aerosaga.repository.DroneRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DroneFleetService {

    private static final Logger logger = LoggerFactory.getLogger(DroneFleetService.class);

    private final DroneRepository droneRepository;

    public DroneFleetService(DroneRepository droneRepository) {
        this.droneRepository = droneRepository;
    }

    @PostConstruct
    public void initFleet() {
        if (droneRepository.count() == 0) {
            logger.info("Initializing baseline drone fleet in database...");
            droneRepository.save(new Drone("DRONE-ALPHA-01", "AeroSky Valkyrie V4", 5.0, 12.9716, 80.2446));
            droneRepository.save(new Drone("DRONE-BETA-02", "HexaCopter Guardian X", 8.5, 12.9716, 80.2446));
            droneRepository.save(new Drone("DRONE-GAMMA-03", "SkyCourier Precision P2", 3.0, 12.9716, 80.2446));
            logger.info("Fleet initialized with 3 operational drones at Base Station coordinates (12.9716, 80.2446).");
        }
    }

    public List<Drone> getAllDrones() {
        return droneRepository.findAll();
    }

    public Drone getDroneById(String droneId) {
        return droneRepository.findById(droneId)
                .orElseThrow(() -> new DroneNotFoundException("Drone not found: " + droneId));
    }

    public Drone registerDrone(Drone drone) {
        if (drone.getId() == null || drone.getId().trim().isEmpty()) {
            drone.setId("DRONE-" + System.currentTimeMillis());
        }
        if (drone.getRegisteredAt() == null) {
            drone.setRegisteredAt(LocalDateTime.now());
        }
        if (drone.getStatus() == null) {
            drone.setStatus(DroneStatus.IDLE);
        }
        return droneRepository.save(drone);
    }

    public Drone rechargeDrone(String droneId) {
        Drone drone = getDroneById(droneId);
        drone.setBatteryPercentage(100.0);
        if (drone.getStatus() == DroneStatus.MAINTENANCE_REQUIRED) {
            drone.setStatus(DroneStatus.IDLE);
            drone.setLastMaintenance(LocalDateTime.now());
        }
        logger.info("Drone {} battery recharged to 100% and cleared for duty.", droneId);
        return droneRepository.save(drone);
    }
}
