package com.infotact.aerosaga.controller;

import com.infotact.aerosaga.dto.MissionCreateDTO;
import com.infotact.aerosaga.dto.MissionDTO;
import com.infotact.aerosaga.model.Mission;
import com.infotact.aerosaga.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
@Tag(name = "Missions", description = "Endpoints for creating, scheduling, and orchestrating drone missions via Temporal.io")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    @Operation(summary = "List all missions (optional status filter)")
    public ResponseEntity<List<MissionDTO>> getAllMissions(@RequestParam(required = false) Mission.Status status) {
        if (status != null) {
            return ResponseEntity.ok(missionService.getMissionsByStatus(status));
        }
        return ResponseEntity.ok(missionService.getAllMissions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get mission details by id")
    public ResponseEntity<MissionDTO> getMissionById(@PathVariable Long id) {
        return ResponseEntity.ok(missionService.getMissionById(id));
    }

    @PostMapping
    @Operation(summary = "Create and schedule a new drone mission")
    public ResponseEntity<MissionDTO> createMission(@Valid @RequestBody MissionCreateDTO dto) {
        return new ResponseEntity<>(missionService.createMission(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update mission lifecycle status")
    public ResponseEntity<MissionDTO> updateStatus(@PathVariable Long id, @RequestParam Mission.Status status) {
        return ResponseEntity.ok(missionService.updateMissionStatus(id, status));
    }

    @PostMapping("/{id}/launch")
    @Operation(summary = "Launch autonomous mission into the Temporal.io Workflow Engine")
    public ResponseEntity<MissionDTO> launchMission(@PathVariable Long id) {
        return ResponseEntity.ok(missionService.launchMissionWorkflow(id));
    }

    @PostMapping("/{id}/abort")
    @Operation(summary = "Send an emergency abort signal to the running Temporal workflow to trigger Saga compensation")
    public ResponseEntity<Map<String, String>> abortMission(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "Emergency abort signaled by operator") String reason) {
        missionService.abortMissionWorkflow(id, reason);
        return ResponseEntity.ok(Map.of("message", "Emergency abort signal dispatched to Temporal workflow engine", "reason", reason));
    }

    @GetMapping("/{id}/temporal-state")
    @Operation(summary = "Query real-time workflow phase and step from the Temporal workflow engine")
    public ResponseEntity<Map<String, Object>> getTemporalState(@PathVariable Long id) {
        return ResponseEntity.ok(missionService.getWorkflowState(id));
    }
}
