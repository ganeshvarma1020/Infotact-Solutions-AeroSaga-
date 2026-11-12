package com.infotact.aerosaga.controller;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.service.DroneFleetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drones")
@Tag(name = "Drone Fleet API", description = "Autonomous Drone Fleet Asset Management and Telemetry Registration")
@CrossOrigin(origins = "*")
public class DroneFleetController {

    private final DroneFleetService droneFleetService;

    public DroneFleetController(DroneFleetService droneFleetService) {
        this.droneFleetService = droneFleetService;
    }

    @GetMapping
    @Operation(summary = "List all registered drone assets")
    public ResponseEntity<List<Drone>> getAllDrones() {
        return ResponseEntity.ok(droneFleetService.getAllDrones());
    }

    @GetMapping("/{droneId}")
    @Operation(summary = "Get specific drone details and status")
    public ResponseEntity<Drone> getDroneById(@PathVariable String droneId) {
        return ResponseEntity.ok(droneFleetService.getDroneById(droneId));
    }

    @PostMapping
    @Operation(summary = "Register a new drone into the fleet")
    public ResponseEntity<Drone> registerDrone(@RequestBody Drone drone) {
        return ResponseEntity.ok(droneFleetService.registerDrone(drone));
    }

    @PostMapping("/{droneId}/recharge")
    @Operation(summary = "Recharge drone battery to 100% and clear maintenance flags")
    public ResponseEntity<Drone> rechargeDrone(@PathVariable String droneId) {
        return ResponseEntity.ok(droneFleetService.rechargeDrone(droneId));
    }
}
