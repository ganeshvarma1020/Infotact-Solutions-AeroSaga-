package com.infotact.aerosaga.service;

import com.infotact.aerosaga.dto.DroneDTO;
import com.infotact.aerosaga.exception.ResourceNotFoundException;
import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.repository.DroneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DroneFleetService {

    private final DroneRepository droneRepository;

    public DroneFleetService(DroneRepository droneRepository) {
        this.droneRepository = droneRepository;
    }

    public List<DroneDTO> getAllDrones() {
        return droneRepository.findAll().stream()
                .map(DroneDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public DroneDTO getDroneById(Long id) {
        Drone drone = droneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drone not found with id: " + id));
        return DroneDTO.fromEntity(drone);
    }

    public List<DroneDTO> getAvailableDrones() {
        return droneRepository.findByStatus(Drone.Status.AVAILABLE).stream()
                .map(DroneDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DroneDTO createDrone(DroneDTO dto) {
        Drone drone = new Drone(
                dto.getName(),
                dto.getModel(),
                dto.getBatteryLevel() != null ? dto.getBatteryLevel() : 100.0,
                dto.getStatus() != null ? dto.getStatus() : Drone.Status.AVAILABLE,
                dto.getCurrentLat(),
                dto.getCurrentLng()
        );
        return DroneDTO.fromEntity(droneRepository.save(drone));
    }

    @Transactional
    public DroneDTO updateDroneStatus(Long id, Drone.Status status) {
        Drone drone = droneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drone not found with id: " + id));
        drone.setStatus(status);
        return DroneDTO.fromEntity(droneRepository.save(drone));
    }

    @Transactional
    public DroneDTO updateDroneTelemetry(Long id, Double lat, Double lng, Double batteryLevel) {
        Drone drone = droneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drone not found with id: " + id));
        if (lat != null) drone.setCurrentLat(lat);
        if (lng != null) drone.setCurrentLng(lng);
        if (batteryLevel != null) drone.setBatteryLevel(batteryLevel);
        return DroneDTO.fromEntity(droneRepository.save(drone));
    }
}
