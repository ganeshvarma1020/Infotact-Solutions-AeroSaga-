package com.infotact.aerosaga.service;

import com.infotact.aerosaga.dto.TelemetryDTO;
import com.infotact.aerosaga.exception.ResourceNotFoundException;
import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.Mission;
import com.infotact.aerosaga.model.Telemetry;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.repository.MissionRepository;
import com.infotact.aerosaga.repository.TelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final MissionRepository missionRepository;
    private final DroneRepository droneRepository;

    public TelemetryService(TelemetryRepository telemetryRepository,
                            MissionRepository missionRepository,
                            DroneRepository droneRepository) {
        this.telemetryRepository = telemetryRepository;
        this.missionRepository = missionRepository;
        this.droneRepository = droneRepository;
    }

    public List<TelemetryDTO> getTelemetryByMission(Long missionId) {
        return telemetryRepository.findByMissionIdOrderByTimestampAsc(missionId).stream()
                .map(TelemetryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public TelemetryDTO getLatestTelemetry(Long missionId) {
        List<Telemetry> list = telemetryRepository.findByMissionIdOrderByTimestampAsc(missionId);
        if (list.isEmpty()) {
            return null;
        }
        return TelemetryDTO.fromEntity(list.get(list.size() - 1));
    }

    @Transactional
    public TelemetryDTO recordTelemetry(TelemetryDTO dto) {
        Mission mission = missionRepository.findById(dto.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + dto.getMissionId()));

        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new ResourceNotFoundException("Drone not found with id: " + dto.getDroneId()));

        Telemetry telemetry = new Telemetry(
                mission,
                drone,
                dto.getBatteryPercentage(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getAltitude(),
                dto.getSpeed()
        );

        // Update current drone telemetry state
        drone.setCurrentLat(dto.getLatitude());
        drone.setCurrentLng(dto.getLongitude());
        drone.setBatteryLevel(dto.getBatteryPercentage());

        return TelemetryDTO.fromEntity(telemetryRepository.save(telemetry));
    }
}
