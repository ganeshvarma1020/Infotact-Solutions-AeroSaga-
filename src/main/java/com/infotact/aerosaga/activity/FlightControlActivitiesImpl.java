package com.infotact.aerosaga.activity;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.DroneStatus;
import com.infotact.aerosaga.model.DroneTelemetry;
import com.infotact.aerosaga.model.Waypoint;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.service.DroneSimulationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FlightControlActivitiesImpl implements FlightControlActivities {

    private static final Logger logger = LoggerFactory.getLogger(FlightControlActivitiesImpl.class);

    private final DroneSimulationEngine simulationEngine;
    private final DroneRepository droneRepository;

    public FlightControlActivitiesImpl(DroneSimulationEngine simulationEngine,
                                       DroneRepository droneRepository) {
        this.simulationEngine = simulationEngine;
        this.droneRepository = droneRepository;
    }

    @Override
    public void initiateTakeoff(String droneId, String missionId, double targetAltitudeMeters) {
        logger.info("[FlightControl] Executing vertical ascent to {} meters for drone {}", targetAltitudeMeters, droneId);
        simulationEngine.setDroneOperationalStatus(droneId, DroneStatus.AIRBORNE, "Ascending to operational altitude");
        try {
            Thread.sleep(600); // simulation flight interval
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("[FlightControl] Drone {} stabilized at cruise altitude of {}m", droneId, targetAltitudeMeters);
    }

    @Override
    public DroneTelemetry flyToWaypoint(String droneId, String missionId, Waypoint waypoint, int waypointIndex, int totalWaypoints) {
        logger.info("[FlightControl] Navigating leg [{}/{}]: Flying to waypoint '{}' ({}, {})",
                waypointIndex + 1, totalWaypoints, waypoint.getLabel(), waypoint.getLatitude(), waypoint.getLongitude());

        Drone drone = droneRepository.findById(droneId).orElse(null);
        double currentBattery = drone != null ? drone.getBatteryPercentage() : 95.0;
        double currentLat = drone != null ? drone.getLatitude() : waypoint.getLatitude();
        double currentLon = drone != null ? drone.getLongitude() : waypoint.getLongitude();
        Waypoint currentPos = new Waypoint(currentLat, currentLon, 15.0, 10.0, "Current Position");

        // Simulate flight transit delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        DroneTelemetry telemetry = simulationEngine.stepTransit(
                droneId, missionId, currentPos, waypoint, currentBattery,
                drone != null ? drone.getCurrentPayloadKg() : 0.0,
                "Transit waypoint " + (waypointIndex + 1)
        );

        waypoint.setReached(true);
        return telemetry;
    }

    @Override
    public void hoverInPlace(String droneId, String missionId, int seconds) {
        logger.info("[FlightControl] Drone {} holding position / hovering for {} seconds", droneId, seconds);
        simulationEngine.setDroneOperationalStatus(droneId, DroneStatus.HOVERING, "Stationary hover hold");
        try {
            Thread.sleep(Math.min(seconds * 500L, 2000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        simulationEngine.setDroneOperationalStatus(droneId, DroneStatus.AIRBORNE, "Resumed flight vector");
    }

    @Override
    public void returnToBase(String droneId, String missionId, Waypoint baseWaypoint) {
        logger.info("[FlightControl] Drone {} initiating automated Return-To-Base (RTB) to Home Base ({}, {})",
                droneId, baseWaypoint.getLatitude(), baseWaypoint.getLongitude());
        simulationEngine.setDroneOperationalStatus(droneId, DroneStatus.RETURNING_HOME, "RTB Vector locked");
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Drone drone = droneRepository.findById(droneId).orElse(null);
        double battery = drone != null ? drone.getBatteryPercentage() : 80.0;
        Waypoint currentPos = new Waypoint(
                drone != null ? drone.getLatitude() : baseWaypoint.getLatitude(),
                drone != null ? drone.getLongitude() : baseWaypoint.getLongitude(),
                30.0, 12.0, "Inbound"
        );
        simulationEngine.stepTransit(droneId, missionId, currentPos, baseWaypoint, battery, 0.0, "RTB arrival");
    }

    @Override
    public void initiateLanding(String droneId, String missionId) {
        logger.info("[FlightControl] Final approach and soft landing initialized for drone {}", droneId);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        simulationEngine.setDroneOperationalStatus(droneId, DroneStatus.IDLE, "Safely touched down at base pad");
        droneRepository.findById(droneId).ifPresent(d -> {
            d.setAltitudeMeters(0.0);
            d.setStatus(DroneStatus.IDLE);
            droneRepository.save(d);
        });
        logger.info("[FlightControl] Drone {} rotors stopped. Securely stationed on pad.", droneId);
    }
}
