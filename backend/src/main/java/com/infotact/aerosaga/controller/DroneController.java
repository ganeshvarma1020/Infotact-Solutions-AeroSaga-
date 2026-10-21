package com.infotact.aerosaga.controller;

import com.infotact.aerosaga.dto.DroneDTO;
import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.service.DroneFleetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drones")
@Tag(name = "Drones", description = "Endpoints for drone fleet management and telemetry monitoring")
public class DroneController {

    private final DroneFleetService droneFleetService;

    public DroneController(DroneFleetService droneFleetService) {
        this.droneFleetService = droneFleetService;
    }

    @GetMapping
    @Operation(summary = "List all drones in the fleet")
    public ResponseEntity<List<DroneDTO>> getAllDrones() {
        return ResponseEntity.ok(droneFleetService.getAllDrones());
    }

    @GetMapping("/available")
    @Operation(summary = "List all drones currently available for assignment")
    public ResponseEntity<List<DroneDTO>> getAvailableDrones() {
        return ResponseEntity.ok(droneFleetService.getAvailableDrones());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific drone details by id")
    public ResponseEntity<DroneDTO> getDroneById(@PathVariable Long id) {
        return ResponseEntity.ok(droneFleetService.getDroneById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new drone in the fleet")
    public ResponseEntity<DroneDTO> createDrone(@Valid @RequestBody DroneDTO dto) {
        return new ResponseEntity<>(droneFleetService.createDrone(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update drone operational status")
    public ResponseEntity<DroneDTO> updateStatus(@PathVariable Long id, @RequestParam Drone.Status status) {
        return ResponseEntity.ok(droneFleetService.updateDroneStatus(id, status));
    }
}
