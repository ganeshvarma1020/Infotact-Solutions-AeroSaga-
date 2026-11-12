package com.infotact.aerosaga.activity;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.repository.DroneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PayloadActivitiesImpl implements PayloadActivities {

    private static final Logger logger = LoggerFactory.getLogger(PayloadActivitiesImpl.class);

    private final DroneRepository droneRepository;

    public PayloadActivitiesImpl(DroneRepository droneRepository) {
        this.droneRepository = droneRepository;
    }

    @Override
    public boolean deployDeliveryPayload(String droneId, String missionId, double payloadWeightKg) {
        logger.info("[Payload] Releasing delivery latch mechanism for mission {} (Weight: {} kg)", missionId, payloadWeightKg);
        try {
            Thread.sleep(700); // physical winch/winch-release simulation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Drone drone = droneRepository.findById(droneId).orElse(null);
        if (drone != null) {
            drone.setCurrentPayloadKg(0.0);
            droneRepository.save(drone);
        }
        logger.info("[Payload] Payload delivery verified: Drop confirmed via pressure sensor release.");
        return true;
    }

    @Override
    public boolean executeSurveillanceScan(String droneId, String missionId, String targetLabel) {
        logger.info("[Payload] Activating optical 4K gimbal camera & LiDAR sensor scanning '{}' for mission {}", targetLabel, missionId);
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("[Payload] 360-degree point-cloud inspection completed for '{}'. Telemetry encrypted & streamed.", targetLabel);
        return true;
    }
}
