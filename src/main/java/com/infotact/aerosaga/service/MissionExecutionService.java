package com.infotact.aerosaga.service;

import com.infotact.aerosaga.config.TemporalConfig;
import com.infotact.aerosaga.exception.MissionExecutionException;
import com.infotact.aerosaga.model.*;
import com.infotact.aerosaga.repository.DroneMissionRepository;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.workflow.DroneMissionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MissionExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(MissionExecutionService.class);

    private final WorkflowClient workflowClient;
    private final TemporalConfig temporalConfig;
    private final DroneMissionRepository missionRepository;
    private final DroneRepository droneRepository;
    private final DroneSimulationEngine simulationEngine;

    public MissionExecutionService(WorkflowClient workflowClient,
                                   TemporalConfig temporalConfig,
                                   DroneMissionRepository missionRepository,
                                   DroneRepository droneRepository,
                                   DroneSimulationEngine simulationEngine) {
        this.workflowClient = workflowClient;
        this.temporalConfig = temporalConfig;
        this.missionRepository = missionRepository;
        this.droneRepository = droneRepository;
        this.simulationEngine = simulationEngine;
    }

    public DroneMission launchMission(MissionRequest request) {
        String droneId = request.getDroneId();
        Drone drone = droneRepository.findById(droneId)
                .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));

        if (drone.getStatus() != DroneStatus.IDLE) {
            throw new IllegalStateException("Drone " + droneId + " is currently " + drone.getStatus() + ". Only IDLE drones can be dispatched.");
        }

        String missionId = "MSN-" + System.currentTimeMillis();
        String workflowId = missionId;

        // Populate JPA mission record
        DroneMission mission = new DroneMission(missionId, droneId, workflowId, request.getMissionType());
        if (request.getWaypoints() != null && !request.getWaypoints().isEmpty()) {
            Waypoint startWp = request.getWaypoints().get(0);
            Waypoint endWp = request.getWaypoints().get(request.getWaypoints().size() - 1);
            mission.setStartLatitude(startWp.getLatitude());
            mission.setStartLongitude(startWp.getLongitude());
            mission.setTargetLatitude(endWp.getLatitude());
            mission.setTargetLongitude(endWp.getLongitude());
        }
        mission.setMissionSummary("Mission " + request.getMissionType() + " dispatched for " + droneId);
        missionRepository.save(mission);

        // Update drone operational state
        drone.setStatus(DroneStatus.PREFLIGHT);
        drone.setCurrentPayloadKg(request.getPayloadWeightKg());
        droneRepository.save(drone);

        // Configure and launch Temporal Workflow
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(temporalConfig.getTaskQueue())
                .build();

        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, options);

        try {
            // Asynchronously start the workflow
            WorkflowClient.start(workflow::executeMission, request);
            logger.info("Launched Temporal Workflow ID [{}] on Task Queue [{}] for mission {}",
                    workflowId, temporalConfig.getTaskQueue(), missionId);
        } catch (Exception e) {
            logger.error("Failed to start Temporal workflow: {}", e.getMessage(), e);
            mission.setStatus(MissionStatus.FAILED);
            mission.setFailureReason("Failed to initiate Temporal workflow: " + e.getMessage());
            missionRepository.save(mission);
            drone.setStatus(DroneStatus.IDLE);
            droneRepository.save(drone);
            throw new MissionExecutionException("Workflow start error: " + e.getMessage(), e);
        }

        return mission;
    }

    public void sendRerouteSignal(String missionId, RerouteRequest reroute) {
        logger.info("Dispatching signalReroute to workflow {}", missionId);
        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, missionId);
        workflow.signalReroute(reroute);
    }

    public void sendPauseSignal(String missionId, int seconds) {
        logger.info("Dispatching signalPauseHover ({}s) to workflow {}", seconds, missionId);
        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, missionId);
        workflow.signalPauseHover(seconds);
    }

    public void sendResumeSignal(String missionId) {
        logger.info("Dispatching signalResume to workflow {}", missionId);
        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, missionId);
        workflow.signalResume();
    }

    public void sendEmergencyAbortSignal(String missionId, String reason) {
        logger.warn("Dispatching signalEmergencyAbort to workflow {}. Reason: {}", missionId, reason);
        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, missionId);
        workflow.signalEmergencyAbort(reason);
    }

    public Map<String, Object> getMissionState(String missionId) {
        Map<String, Object> result = new LinkedHashMap<>();
        DroneMission mission = missionRepository.findById(missionId).orElse(null);
        result.put("mission", mission);

        try {
            DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, missionId);
            MissionStatus liveStatus = workflow.queryMissionStatus();
            String livePhase = workflow.queryCurrentPhase();
            DroneTelemetry liveTelemetry = workflow.queryCurrentTelemetry();
            List<Waypoint> waypoints = workflow.queryActiveWaypoints();

            result.put("liveStatus", liveStatus);
            result.put("currentPhase", livePhase);
            result.put("telemetry", liveTelemetry);
            result.put("activeWaypoints", waypoints);
            result.put("isLiveWorkflowActive", true);

            // Synchronize JPA status if changed
            if (mission != null && mission.getStatus() != liveStatus) {
                mission.setStatus(liveStatus);
                if (liveStatus == MissionStatus.COMPLETED || liveStatus == MissionStatus.ABORTED_SAFE_LANDING) {
                    mission.setEndTime(LocalDateTime.now());
                }
                missionRepository.save(mission);
            }
        } catch (Exception e) {
            // Workflow might have completed or stopped
            result.put("isLiveWorkflowActive", false);
            result.put("note", "Workflow execution completed or not currently running in Temporal queue: " + e.getMessage());
            if (mission != null) {
                DroneTelemetry cachedTelemetry = simulationEngine.getLiveTelemetry(mission.getDroneId());
                result.put("telemetry", cachedTelemetry);
            }
        }

        return result;
    }

    public List<DroneMission> getAllMissions() {
        return missionRepository.findAll();
    }
}
