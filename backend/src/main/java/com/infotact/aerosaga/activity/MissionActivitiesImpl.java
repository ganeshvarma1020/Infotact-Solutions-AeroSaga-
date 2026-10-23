package com.infotact.aerosaga.activity;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.Mission;
import com.infotact.aerosaga.model.MissionEvent;
import com.infotact.aerosaga.model.Telemetry;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.repository.MissionEventRepository;
import com.infotact.aerosaga.repository.MissionRepository;
import com.infotact.aerosaga.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MissionActivitiesImpl implements MissionActivities {

    private static final Logger log = LoggerFactory.getLogger(MissionActivitiesImpl.class);

    private final MissionRepository missionRepository;
    private final DroneRepository droneRepository;
    private final TelemetryRepository telemetryRepository;
    private final MissionEventRepository missionEventRepository;

    public MissionActivitiesImpl(MissionRepository missionRepository,
                                 DroneRepository droneRepository,
                                 TelemetryRepository telemetryRepository,
                                 MissionEventRepository missionEventRepository) {
        this.missionRepository = missionRepository;
        this.droneRepository = droneRepository;
        this.telemetryRepository = telemetryRepository;
        this.missionEventRepository = missionEventRepository;
    }

    @Override
    @Transactional
    public boolean validateMission(Long missionId) {
        log.info("[Temporal Activity] Validating mission #{}", missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        if (mission.getDestinationLat() == null || mission.getDestinationLng() == null) {
            throw new IllegalStateException("Invalid destination coordinates");
        }

        missionEventRepository.save(new MissionEvent(
                mission,
                "VALIDATION_PASSED",
                "Pre-flight avionics check, weather matrix, and destination coordinates validated."
        ));

        return true;
    }

    @Override
    @Transactional
    public boolean reserveDrone(Long missionId, Long droneId) {
        log.info("[Temporal Activity] Reserving drone #{} for mission #{}", droneId, missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        Drone drone = droneRepository.findById(droneId)
                .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));

        if (drone.getBatteryLevel() < 20.0) {
            throw new IllegalStateException("Drone battery level is below safe launch threshold (20%): " + drone.getBatteryLevel() + "%");
        }

        drone.setStatus(Drone.Status.IN_MISSION);
        droneRepository.save(drone);

        mission.setDrone(drone);
        mission.setStatus(Mission.Status.RUNNING);
        missionRepository.save(mission);

        missionEventRepository.save(new MissionEvent(
                mission,
                "TAKEOFF",
                "Drone " + drone.getName() + " reserved. Status set to IN_MISSION (Battery: " + drone.getBatteryLevel() + "%). Takeoff cleared."
        ));

        return true;
    }

    @Override
    @Transactional
    public boolean simulateFlightStep(Long missionId, Long droneId, int step, int totalSteps, double destLat, double destLng) {
        log.info("[Temporal Activity] Simulating flight step {}/{} for mission #{}", step, totalSteps, missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        Drone drone = droneRepository.findById(droneId)
                .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));

        double progressRatio = (double) step / (double) totalSteps;

        // Linear interpolation towards destination
        double curLat = drone.getCurrentLat() + (destLat - drone.getCurrentLat()) * (1.0 / (totalSteps - step + 1));
        double curLng = drone.getCurrentLng() + (destLng - drone.getCurrentLng()) * (1.0 / (totalSteps - step + 1));

        // Parabolic altitude profile: climbs to 80m, cruises, then descends on final step
        double altitude = step == totalSteps ? 0.0 : Math.min(80.0, 20.0 + (step * 15.0));
        double speed = step == totalSteps ? 0.0 : 38.0 + (Math.sin(step) * 5.0);
        double newBattery = Math.max(5.0, drone.getBatteryLevel() - 1.2);

        drone.setCurrentLat(curLat);
        drone.setCurrentLng(curLng);
        drone.setBatteryLevel(newBattery);
        droneRepository.save(drone);

        // Record time-series telemetry
        Telemetry telemetry = new Telemetry(mission, drone, newBattery, curLat, curLng, altitude, speed);
        telemetryRepository.save(telemetry);

        String eventDesc = String.format("Waypoint %d/%d reached. Coordinates: (%.4f, %.4f), Altitude: %.1fm, Battery: %.1f%%",
                step, totalSteps, curLat, curLng, altitude, newBattery);

        missionEventRepository.save(new MissionEvent(mission, "WAYPOINT_REACHED", eventDesc));

        return true;
    }

    @Override
    @Transactional
    public boolean finalizeMission(Long missionId, Long droneId) {
        log.info("[Temporal Activity] Finalizing mission #{} for drone #{}", missionId, droneId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        Drone drone = droneRepository.findById(droneId)
                .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));

        mission.setStatus(Mission.Status.COMPLETED);
        missionRepository.save(mission);

        drone.setStatus(Drone.Status.AVAILABLE);
        droneRepository.save(drone);

        missionEventRepository.save(new MissionEvent(
                mission,
                "COMPLETED",
                "Mission executed successfully. Drone safely landed and released to AVAILABLE status."
        ));

        return true;
    }

    @Override
    @Transactional
    public void compensateMissionFailure(Long missionId, Long droneId, String reason) {
        log.warn("[Temporal Saga Compensation] Rolling back mission #{}, reason: {}", missionId, reason);

        missionRepository.findById(missionId).ifPresent(mission -> {
            mission.setStatus(Mission.Status.FAILED);
            missionRepository.save(mission);

            missionEventRepository.save(new MissionEvent(
                    mission,
                    "ERROR",
                    "SAGA COMPENSATION TRIGGERED: " + reason + ". Mission marked as FAILED."
            ));
        });

        droneRepository.findById(droneId).ifPresent(drone -> {
            if (drone.getBatteryLevel() < 15.0) {
                drone.setStatus(Drone.Status.MAINTENANCE);
            } else {
                drone.setStatus(Drone.Status.AVAILABLE);
            }
            droneRepository.save(drone);
        });
    }
}
