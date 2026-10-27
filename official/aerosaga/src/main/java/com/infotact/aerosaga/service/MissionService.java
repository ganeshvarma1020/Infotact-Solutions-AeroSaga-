package com.infotact.aerosaga.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.infotact.aerosaga.api.ApiDtos.CreateMissionRequest;
import com.infotact.aerosaga.api.ApiDtos.EventView;
import com.infotact.aerosaga.api.ApiDtos.MissionSummary;
import com.infotact.aerosaga.api.ApiException;
import com.infotact.aerosaga.config.AeroSagaProperties;
import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.MissionPlanValidator;
import com.infotact.aerosaga.domain.MissionRequest;
import com.infotact.aerosaga.domain.ValidationResult;
import com.infotact.aerosaga.domain.Waypoint;
import com.infotact.aerosaga.fleet.DroneFleet;
import com.infotact.aerosaga.persistence.MissionEventRepository;
import com.infotact.aerosaga.persistence.MissionRecord;
import com.infotact.aerosaga.persistence.MissionRepository;
import com.infotact.aerosaga.workflow.MissionInput;
import com.infotact.aerosaga.workflow.MissionPhase;
import com.infotact.aerosaga.workflow.MissionView;
import com.infotact.aerosaga.workflow.MissionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;

/** Everything the API needs: start missions, read their live state, abort them, list history. */
@Service
public class MissionService {

    private static final Logger log = LoggerFactory.getLogger(MissionService.class);

    private final WorkflowClient client;
    private final AeroSagaProperties props;
    private final MissionPlanValidator validator;
    private final DroneFleet fleet;
    private final MissionRepository missions;
    private final MissionEventRepository events;

    public MissionService(WorkflowClient client, AeroSagaProperties props, MissionPlanValidator validator,
                          DroneFleet fleet, MissionRepository missions, MissionEventRepository events) {
        this.client = client;
        this.props = props;
        this.validator = validator;
        this.fleet = fleet;
        this.missions = missions;
        this.events = events;
    }

    /**
     * 1. Build the domain request with a new id.
     * 2. Validate now, so the operator gets errors immediately (the workflow checks again).
     * 3. Save a SUBMITTED row, then start the workflow.
     */
    public String submit(CreateMissionRequest body) {
        String id = "M-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        List<Waypoint> wps = body.waypoints().stream()
                .map(w -> new Waypoint(w.name().trim(), new GeoPoint(w.lat(), w.lon()))).toList();
        FailureScenario scenario = body.scenario() == null ? FailureScenario.NONE : body.scenario();
        MissionRequest request = new MissionRequest(id, body.operator().trim(), fleet.home(), wps, body.payloadKg(), scenario);

        ValidationResult check = validator.validate(request);
        if (!check.valid()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Mission plan breaks operating rules", check.errors());
        }

        MissionRecord record = new MissionRecord(id, request.operator(), request.payloadKg(), wps.size(),
                round2(request.route().totalKm()), scenario.name(), MissionPhase.SUBMITTED.name(), Instant.now());
        missions.save(record);

        MissionWorkflow workflow = client.newWorkflowStub(MissionWorkflow.class, WorkflowOptions.newBuilder()
                .setWorkflowId(id)
                .setTaskQueue(props.temporal().taskQueue())
                .setWorkflowExecutionTimeout(Duration.ofHours(6))
                .build());
        try {
            WorkflowClient.start(workflow::execute, new MissionInput(request, props.simulation().timeScale()));
        } catch (RuntimeException e) {
            log.error("Could not start workflow {}: {}", id, e.getMessage());
            record.finish(MissionPhase.FAILED_TO_START.name(), null, "TEMPORAL_UNAVAILABLE", e.getMessage(), Instant.now());
            missions.save(record);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Mission control engine (Temporal) is unreachable. Mission " + id + " was not started.");
        }
        log.info("Mission {} started ({} waypoints, scenario {})", id, wps.size(), scenario);
        return id;
    }

    /** Live state from the workflow. If the workflow is gone, fall back to the database row. */
    public MissionView status(String id) {
        try {
            return client.newWorkflowStub(MissionWorkflow.class, id).status();
        } catch (WorkflowNotFoundException e) {
            return fromDatabase(id);
        } catch (RuntimeException e) {
            log.warn("Live status for {} unavailable: {}", id, e.getMessage());
            return fromDatabase(id);
        }
    }

    public void abort(String id, String reason) {
        MissionRecord record = missions.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mission " + id + " not found"));
        if (MissionPhase.valueOf(record.getPhase()).isFinal()) {
            throw new ApiException(HttpStatus.CONFLICT, "Mission " + id + " already ended (" + record.getPhase() + ")");
        }
        try {
            client.newWorkflowStub(MissionWorkflow.class, id).abort(reason);
        } catch (WorkflowNotFoundException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Mission " + id + " is no longer running");
        }
    }

    public List<MissionSummary> recent() {
        return missions.findTop50ByOrderByCreatedAtDesc().stream()
                .map(m -> new MissionSummary(m.getId(), m.getOperator(), m.getPhase(), m.getScenario(), m.getDroneId(),
                        m.getPayloadKg(), m.getRouteKm(), m.getFailureCode(), m.getCreatedAt(), m.getFinishedAt()))
                .toList();
    }

    public List<EventView> history(String id) {
        if (!missions.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Mission " + id + " not found");
        }
        return events.findByMissionIdOrderByOccurredAtAscIdAsc(id).stream()
                .map(e -> new EventView(e.getPhase(), e.getMessage(), e.getOccurredAt())).toList();
    }

    private MissionView fromDatabase(String id) {
        MissionRecord m = missions.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mission " + id + " not found"));
        return new MissionView(m.getId(), MissionPhase.valueOf(m.getPhase()), FailureScenario.valueOf(m.getScenario()),
                m.getPayloadKg(), m.getDroneId(), null, 0, m.getWaypointCount(), null, null, null, 0, false,
                m.getFailureCode(), m.getFailureReason(), List.of(), List.of());
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
