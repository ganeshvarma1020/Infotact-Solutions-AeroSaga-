package com.infotact.aerosaga.controller;

import com.infotact.aerosaga.dto.TelemetryDTO;
import com.infotact.aerosaga.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
@Tag(name = "Telemetry", description = "Endpoints for telemetry streaming and short-polling")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/mission/{missionId}")
    @Operation(summary = "Get full telemetry trajectory history for a mission")
    public ResponseEntity<List<TelemetryDTO>> getTelemetryByMission(@PathVariable Long missionId) {
        return ResponseEntity.ok(telemetryService.getTelemetryByMission(missionId));
    }

    @GetMapping("/mission/{missionId}/latest")
    @Operation(summary = "Get latest telemetry snapshot for short-polling by the React dashboard")
    public ResponseEntity<TelemetryDTO> getLatestTelemetry(@PathVariable Long missionId) {
        TelemetryDTO latest = telemetryService.getLatestTelemetry(missionId);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    @PostMapping
    @Operation(summary = "Ingest a new telemetry data point")
    public ResponseEntity<TelemetryDTO> recordTelemetry(@Valid @RequestBody TelemetryDTO dto) {
        return new ResponseEntity<>(telemetryService.recordTelemetry(dto), HttpStatus.CREATED);
    }
}
