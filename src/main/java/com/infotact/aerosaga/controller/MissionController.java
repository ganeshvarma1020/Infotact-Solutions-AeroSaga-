package com.infotact.aerosaga.controller;

import com.infotact.aerosaga.model.*;
import com.infotact.aerosaga.repository.TelemetryRecordRepository;
import com.infotact.aerosaga.service.MissionExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
@Tag(name = "Mission Control API", description = "Autonomous Drone Mission Planning, Execution, and Temporal Orchestration")
@CrossOrigin(origins = "*")
public class MissionController {

    private final MissionExecutionService missionExecutionService;
    private final TelemetryRecordRepository telemetryRecordRepository;

    public MissionController(MissionExecutionService missionExecutionService,
                             TelemetryRecordRepository telemetryRecordRepository) {
        this.missionExecutionService = missionExecutionService;
        this.telemetryRecordRepository = telemetryRecordRepository;
    }

    @PostMapping("/launch")
    @Operation(summary = "Launch a new autonomous drone mission workflow")
    public ResponseEntity<DroneMission> launchMission(@RequestBody MissionRequest request) {
        DroneMission mission = missionExecutionService.launchMission(request);
        return ResponseEntity.ok(mission);
    }

    @GetMapping
    @Operation(summary = "List all historical and active missions")
    public ResponseEntity<List<DroneMission>> getAllMissions() {
        return ResponseEntity.ok(missionExecutionService.getAllMissions());
    }

    @GetMapping("/{missionId}")
    @Operation(summary = "Get detailed mission state and live Temporal workflow query state")
    public ResponseEntity<Map<String, Object>> getMissionDetails(@PathVariable String missionId) {
        return ResponseEntity.ok(missionExecutionService.getMissionState(missionId));
    }

    @PostMapping("/{missionId}/signal/reroute")
    @Operation(summary = "Dispatch Temporal Signal to update flight waypoints dynamically")
    public ResponseEntity<Map<String, String>> signalReroute(@PathVariable String missionId,
                                                             @RequestBody RerouteRequest reroute) {
        missionExecutionService.sendRerouteSignal(missionId, reroute);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Reroute signal sent to workflow " + missionId));
    }

    @PostMapping("/{missionId}/signal/pause")
    @Operation(summary = "Dispatch Temporal Signal to hover in place for given seconds")
    public ResponseEntity<Map<String, String>> signalPause(@PathVariable String missionId,
                                                           @RequestParam(defaultValue = "10") int seconds) {
        missionExecutionService.sendPauseSignal(missionId, seconds);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Hover hold signal dispatched: " + seconds + "s"));
    }

    @PostMapping("/{missionId}/signal/resume")
    @Operation(summary = "Dispatch Temporal Signal to resume flight navigation")
    public ResponseEntity<Map<String, String>> signalResume(@PathVariable String missionId) {
        missionExecutionService.sendResumeSignal(missionId);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Resume flight signal dispatched."));
    }

    @PostMapping("/{missionId}/signal/abort")
    @Operation(summary = "Dispatch Temporal Signal to trigger emergency abort and execute Saga compensation")
    public ResponseEntity<Map<String, String>> signalEmergencyAbort(@PathVariable String missionId,
                                                                    @RequestParam(defaultValue = "Command Center manual emergency abort") String reason) {
        missionExecutionService.sendEmergencyAbortSignal(missionId, reason);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Emergency abort signal sent. Saga compensating..."));
    }

    @GetMapping("/{missionId}/telemetry")
    @Operation(summary = "Get historical telemetry records for a mission")
    public ResponseEntity<List<TelemetryRecord>> getMissionTelemetry(@PathVariable String missionId) {
        return ResponseEntity.ok(telemetryRecordRepository.findByMissionIdOrderByRecordedAtAsc(missionId));
    }
}
