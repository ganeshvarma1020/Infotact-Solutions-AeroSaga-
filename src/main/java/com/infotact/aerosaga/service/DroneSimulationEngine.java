package com.infotact.aerosaga.service;

import com.infotact.aerosaga.model.*;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.repository.TelemetryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Physics and telemetry simulation engine for autonomous drone operations.
 */
@Service
public class DroneSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(DroneSimulationEngine.class);

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final DroneRepository droneRepository;

    // In-memory real-time state cache for high-frequency queries
    private final Map<String, DroneTelemetry> liveTelemetryMap = new ConcurrentHashMap<>();

    public DroneSimulationEngine(TelemetryRecordRepository telemetryRecordRepository,
                                 DroneRepository droneRepository) {
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.droneRepository = droneRepository;
    }

    public DroneTelemetry getLiveTelemetry(String droneId) {
        return liveTelemetryMap.get(droneId);
    }

    public void updateLiveTelemetry(DroneTelemetry telemetry) {
        liveTelemetryMap.put(telemetry.getDroneId(), telemetry);
    }

    public DroneTelemetry stepTransit(String droneId, String missionId, Waypoint currentPos, Waypoint targetPos,
                                      double currentBattery, double payloadKg, String stepNote) {
        // Calculate bearing and heading
        double heading = calculateBearingDegrees(currentPos.getLatitude(), currentPos.getLongitude(),
                targetPos.getLatitude(), targetPos.getLongitude());

        // Distance in km
        double distanceKm = GeofenceService.calculateHaversineDistanceKm(
                currentPos.getLatitude(), currentPos.getLongitude(),
                targetPos.getLatitude(), targetPos.getLongitude()
        );

        // Realistic battery drain calculation: 0.8% per km + 0.1% per kg payload
        double drain = (distanceKm * 0.8) + (payloadKg * 0.1);
        double updatedBattery = Math.round(Math.max(0.0, currentBattery - drain) * 10.0) / 10.0;

        double voltage = 22.2 * (updatedBattery / 100.0) + 2.0; // 6S LiPo approximate
        voltage = Math.round(voltage * 100.0) / 100.0;

        DroneTelemetry telemetry = new DroneTelemetry(
                droneId,
                missionId,
                targetPos.getLatitude(),
                targetPos.getLongitude(),
                targetPos.getAltitudeMeters(),
                targetPos.getSpeedMps(),
                Math.round(heading * 10.0) / 10.0,
                updatedBattery,
                voltage,
                42.5, // 42.5 deg C motor temp
                16,   // 16 satellites GPS lock
                8.4,  // wind knots
                DroneStatus.AIRBORNE,
                targetPos.getLabel()
        );

        liveTelemetryMap.put(droneId, telemetry);

        // Persist to database record
        TelemetryRecord record = new TelemetryRecord(
                missionId,
                droneId,
                targetPos.getLatitude(),
                targetPos.getLongitude(),
                targetPos.getAltitudeMeters(),
                targetPos.getSpeedMps(),
                updatedBattery,
                stepNote + " -> " + targetPos.getLabel()
        );
        telemetryRecordRepository.save(record);

        // Update Drone entity state
        droneRepository.findById(droneId).ifPresent(drone -> {
            drone.setLatitude(targetPos.getLatitude());
            drone.setLongitude(targetPos.getLongitude());
            drone.setAltitudeMeters(targetPos.getAltitudeMeters());
            drone.setBatteryPercentage(updatedBattery);
            drone.setStatus(DroneStatus.AIRBORNE);
            droneRepository.save(drone);
        });

        logger.info("Drone {} reached {} | Battery: {}% | Alt: {}m | Heading: {}°",
                droneId, targetPos.getLabel(), updatedBattery, targetPos.getAltitudeMeters(), heading);

        return telemetry;
    }

    public void setDroneOperationalStatus(String droneId, DroneStatus status, String note) {
        DroneTelemetry telemetry = liveTelemetryMap.get(droneId);
        if (telemetry != null) {
            telemetry.setStatus(status);
        }
        droneRepository.findById(droneId).ifPresent(drone -> {
            drone.setStatus(status);
            droneRepository.save(drone);
        });
        logger.info("Drone {} status changed to {}: {}", droneId, status, note);
    }

    public double calculateBearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) -
                   Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);
        double theta = Math.atan2(y, x);
        return (Math.toDegrees(theta) + 360) % 360;
    }
}
