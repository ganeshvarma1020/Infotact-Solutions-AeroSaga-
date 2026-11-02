package com.infotact.aerosaga.controller;

import com.infotact.aerosaga.model.*;
import com.infotact.aerosaga.repository.DroneMissionRepository;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard API", description = "Fleet Analytics and Pre-configured Simulation Scenarios")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DroneRepository droneRepository;
    private final DroneMissionRepository missionRepository;
    private final GeofenceService geofenceService;

    public DashboardController(DroneRepository droneRepository,
                               DroneMissionRepository missionRepository,
                               GeofenceService geofenceService) {
        this.droneRepository = droneRepository;
        this.missionRepository = missionRepository;
        this.geofenceService = geofenceService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get global fleet and mission operational metrics")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        List<Drone> drones = droneRepository.findAll();
        List<DroneMission> missions = missionRepository.findAll();

        long idleCount = drones.stream().filter(d -> d.getStatus() == DroneStatus.IDLE).count();
        long activeCount = drones.stream().filter(d -> d.getStatus() == DroneStatus.AIRBORNE || d.getStatus() == DroneStatus.PREFLIGHT || d.getStatus() == DroneStatus.HOVERING).count();
        long maintCount = drones.stream().filter(d -> d.getStatus() == DroneStatus.MAINTENANCE_REQUIRED || d.getStatus() == DroneStatus.EMERGENCY_LANDING).count();

        long completedMissions = missions.stream().filter(m -> m.getStatus() == MissionStatus.COMPLETED).count();
        long compensatedMissions = missions.stream().filter(DroneMission::isSagaCompensated).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDrones", drones.size());
        summary.put("idleDrones", idleCount);
        summary.put("activeDrones", activeCount);
        summary.put("maintenanceDrones", maintCount);
        summary.put("totalMissions", missions.size());
        summary.put("completedMissions", completedMissions);
        summary.put("compensatedMissions", compensatedMissions);
        summary.put("restrictedZones", geofenceService.getRestrictedZones());

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/fleet-report")
    @Operation(summary = "Generate detailed real-time active vs inactive fleet analytics report")
    public ResponseEntity<Map<String, Object>> getFleetReport() {
        List<Drone> drones = droneRepository.findAll();

        List<Drone> activeList = drones.stream()
                .filter(d -> d.getStatus() == DroneStatus.AIRBORNE ||
                             d.getStatus() == DroneStatus.HOVERING ||
                             d.getStatus() == DroneStatus.PREFLIGHT ||
                             d.getStatus() == DroneStatus.RETURNING_HOME)
                .toList();

        List<Drone> idleList = drones.stream()
                .filter(d -> d.getStatus() == DroneStatus.IDLE)
                .toList();

        List<Drone> maintenanceList = drones.stream()
                .filter(d -> d.getStatus() == DroneStatus.MAINTENANCE_REQUIRED ||
                             d.getStatus() == DroneStatus.EMERGENCY_LANDING)
                .toList();

        double avgBattery = drones.stream()
                .mapToDouble(Drone::getBatteryPercentage)
                .average()
                .orElse(100.0);

        int total = drones.size();
        int active = activeList.size();
        int inactive = idleList.size();
        int maintenance = maintenanceList.size();
        double operationalRate = total > 0 ? ((double) (active + inactive) / total) * 100.0 : 0.0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalDrones", total);
        report.put("activeDronesCount", active);
        report.put("inactiveDronesCount", inactive);
        report.put("maintenanceDronesCount", maintenance);
        report.put("operationalRatePercent", Math.round(operationalRate * 10.0) / 10.0);
        report.put("averageBatteryPercentage", Math.round(avgBattery * 10.0) / 10.0);
        report.put("activeDrones", activeList);
        report.put("inactiveDrones", idleList);
        report.put("maintenanceDrones", maintenanceList);
        report.put("reportTimestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(report);
    }

    @GetMapping("/scenarios")
    @Operation(summary = "Get ready-to-launch demonstration mission presets")
    public ResponseEntity<List<Map<String, Object>>> getDemoScenarios() {
        List<Map<String, Object>> scenarios = new ArrayList<>();

        // Scenario 1: Standard Medical Delivery
        Map<String, Object> s1 = new LinkedHashMap<>();
        s1.put("id", "SCENARIO_1_STANDARD_DELIVERY");
        s1.put("title", "1. Standard Medical Supply Delivery");
        s1.put("description", "Dispatches Drone Alpha on a nominal 3-waypoint urban delivery corridor with automated package winch drop and safe return to base.");
        s1.put("missionType", MissionType.MEDICAL_SUPPLY_DELIVERY);
        s1.put("droneId", "DRONE-ALPHA-01");
        s1.put("payloadWeightKg", 2.5);
        s1.put("simulateSevereWeather", false);
        s1.put("simulateMotorFailure", false);
        s1.put("waypoints", List.of(
                new Waypoint(12.9716, 80.2446, 35.0, 12.0, "Base Station Launch Pad"),
                new Waypoint(12.9780, 80.2510, 40.0, 14.0, "Waypoint Alpha (Corridor North)"),
                new Waypoint(12.9835, 80.2580, 20.0, 8.0, "Hospital Helipad Drop Zone"),
                new Waypoint(12.9716, 80.2446, 35.0, 15.0, "Base Station Home Arrival")
        ));
        scenarios.add(s1);

        // Scenario 2: Critical Infrastructure Surveillance
        Map<String, Object> s2 = new LinkedHashMap<>();
        s2.put("id", "SCENARIO_2_SURVEILLANCE");
        s2.put("title", "2. Critical Infrastructure Surveillance");
        s2.put("description", "Perimeter security inspection of coastal utility sub-stations using optical 4K and LiDAR scanning.");
        s2.put("missionType", MissionType.CRITICAL_INFRASTRUCTURE_SURVEILLANCE);
        s2.put("droneId", "DRONE-BETA-02");
        s2.put("payloadWeightKg", 1.0);
        s2.put("simulateSevereWeather", false);
        s2.put("simulateMotorFailure", false);
        s2.put("waypoints", List.of(
                new Waypoint(12.9716, 80.2446, 30.0, 10.0, "Base Station Launch Pad"),
                new Waypoint(12.9650, 80.2490, 45.0, 12.0, "Substation 1 Perimeter"),
                new Waypoint(12.9600, 80.2550, 50.0, 12.0, "High-Voltage Transmission Pylon"),
                new Waypoint(12.9716, 80.2446, 30.0, 14.0, "Base Station Home Arrival")
        ));
        scenarios.add(s2);

        // Scenario 3: Extreme Weather Abort & Temporal Saga Compensation
        Map<String, Object> s3 = new LinkedHashMap<>();
        s3.put("id", "SCENARIO_3_WEATHER_SAGA");
        s3.put("title", "3. Severe Weather Hazard -> Temporal Saga Rollback");
        s3.put("description", "Simulates mid-air severe wind squall. Temporal Saga intercepts the fault and triggers automated emergency compensations: safe touchdown, payload lockdown, and ATC beacon.");
        s3.put("missionType", MissionType.MEDICAL_SUPPLY_DELIVERY);
        s3.put("droneId", "DRONE-GAMMA-03");
        s3.put("payloadWeightKg", 1.8);
        s3.put("simulateSevereWeather", true);
        s3.put("simulateMotorFailure", false);
        s3.put("waypoints", List.of(
                new Waypoint(12.9716, 80.2446, 35.0, 10.0, "Base Station Launch Pad"),
                new Waypoint(12.9750, 80.2480, 40.0, 12.0, "Storm Boundary Entry Point"),
                new Waypoint(12.9800, 80.2520, 25.0, 10.0, "Remote Medical Post")
        ));
        scenarios.add(s3);

        // Scenario 4: Motor Anomaly Fail-Safe & Compensation
        Map<String, Object> s4 = new LinkedHashMap<>();
        s4.put("id", "SCENARIO_4_MOTOR_FAILURE");
        s4.put("title", "4. In-Flight Motor Anomaly -> Immediate Safe Touchdown");
        s4.put("description", "Simulates unexpected rotor degradation. Triggers automated emergency landing activity and marks drone for maintenance inspection.");
        s4.put("missionType", MissionType.PERIMETER_SECURITY_PATROL);
        s4.put("droneId", "DRONE-ALPHA-01");
        s4.put("payloadWeightKg", 0.5);
        s4.put("simulateSevereWeather", false);
        s4.put("simulateMotorFailure", true);
        s4.put("waypoints", List.of(
                new Waypoint(12.9716, 80.2446, 30.0, 10.0, "Base Station Launch Pad"),
                new Waypoint(12.9740, 80.2470, 35.0, 12.0, "Urban Sector 4 Patrol"),
                new Waypoint(12.9780, 80.2500, 35.0, 12.0, "Urban Sector 5 Patrol")
        ));
        scenarios.add(s4);

        return ResponseEntity.ok(scenarios);
    }
}
