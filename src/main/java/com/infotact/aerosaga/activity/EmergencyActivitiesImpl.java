package com.infotact.aerosaga.activity;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.DroneMission;
import com.infotact.aerosaga.model.DroneStatus;
import com.infotact.aerosaga.model.MissionStatus;
import com.infotact.aerosaga.repository.DroneMissionRepository;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.service.DroneSimulationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EmergencyActivitiesImpl implements EmergencyActivities {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyActivitiesImpl.class);

    private final DroneRepository droneRepository;
    private final DroneMissionRepository droneMissionRepository;
    private final DroneSimulationEngine simulationEngine;

    public EmergencyActivitiesImpl(DroneRepository droneRepository,
                                  DroneMissionRepository droneMissionRepository,
                                  DroneSimulationEngine simulationEngine) {
        this.droneRepository = droneRepository;
        this.droneMissionRepository = droneMissionRepository;
        this.simulationEngine = simulationEngine;
    }

    @Override
    public void executeEmergencySafeTouchdown(String droneId, String missionId, String failureReason) {
        logger.error("[Saga Compensation] INITIATING EMERGENCY TOUCHDOWN PROTOCOL for drone {} | Reason: {}",
                droneId, failureReason);

        simulationEngine.setDroneOperationalStatus(droneId, DroneStatus.EMERGENCY_LANDING,
                "Fail-safe emergency descent engaged: " + failureReason);

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        droneRepository.findById(droneId).ifPresent(drone -> {
            drone.setStatus(DroneStatus.MAINTENANCE_REQUIRED);
            drone.setAltitudeMeters(0.0);
            droneRepository.save(drone);
        });

        logger.warn("[Saga Compensation] Drone {} safely touched down at emergency buffer coordinate. Engines disengaged.", droneId);
    }

    @Override
    public void securePayloadLockdown(String droneId, String missionId) {
        logger.info("[Saga Compensation] Locking payload retention clamps and engaging sensor covers for drone {}", droneId);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("[Saga Compensation] Payload locked and protected against impact.");
    }

    @Override
    public void broadcastAirspaceEmergencyBeacon(String droneId, String missionId, String emergencyReason) {
        logger.warn("[Saga Compensation] Broadcaster squawking Emergency Transponder Code 7700 for drone {}. Reason: {}",
                droneId, emergencyReason);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.warn("[Saga Compensation] Air Traffic Control (ATC) notified of drone emergency descent.");
    }

    @Override
    public void recordMissionCompensation(String missionId, String compensationDetails) {
        logger.info("[Saga Compensation] Updating audit trail for mission {}: {}", missionId, compensationDetails);
        droneMissionRepository.findById(missionId).ifPresent(mission -> {
            mission.setStatus(MissionStatus.ABORTED_SAFE_LANDING);
            mission.setSagaCompensated(true);
            mission.setFailureReason(compensationDetails);
            mission.setEndTime(LocalDateTime.now());
            droneMissionRepository.save(mission);
        });
        logger.info("[Saga Compensation] Database state synchronized: Mission marked as SAGA_COMPENSATED.");
    }
}
