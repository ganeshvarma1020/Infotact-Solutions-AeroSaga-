package com.infotact.aerosaga.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infotact.aerosaga.api.ApiDtos.AbortRequest;
import com.infotact.aerosaga.api.ApiDtos.CreateMissionRequest;
import com.infotact.aerosaga.api.ApiDtos.EventView;
import com.infotact.aerosaga.api.ApiDtos.MissionSummary;
import com.infotact.aerosaga.service.MissionService;
import com.infotact.aerosaga.workflow.MissionView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService service;

    public MissionController(MissionService service) {
        this.service = service;
    }

    /** Start a mission. 202 Accepted: it runs in the background; poll GET /api/missions/{id}. */
    @PostMapping
    public ResponseEntity<MissionView> create(@Valid @RequestBody CreateMissionRequest body) {
        String id = service.submit(body);
        return ResponseEntity.accepted().location(URI.create("/api/missions/" + id)).body(service.status(id));
    }

    @GetMapping
    public List<MissionSummary> list() {
        return service.recent();
    }

    @GetMapping("/{id}")
    public MissionView get(@PathVariable String id) {
        return service.status(id);
    }

    @GetMapping("/{id}/events")
    public List<EventView> events(@PathVariable String id) {
        return service.history(id);
    }

    @PostMapping("/{id}/abort")
    public ResponseEntity<Void> abort(@PathVariable String id, @Valid @RequestBody(required = false) AbortRequest body) {
        service.abort(id, body == null ? null : body.reason());
        return ResponseEntity.accepted().build();
    }
}
