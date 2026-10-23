package com.infotact.aerosaga.service;

import com.infotact.aerosaga.config.TemporalConfig;
import com.infotact.aerosaga.dto.MissionCreateDTO;
import com.infotact.aerosaga.dto.MissionDTO;
import com.infotact.aerosaga.exception.BadRequestException;
import com.infotact.aerosaga.exception.ResourceNotFoundException;
import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.Mission;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.repository.MissionRepository;
import com.infotact.aerosaga.workflow.DroneMissionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MissionService {

    private static final Logger log = LoggerFactory.getLogger(MissionService.class);

    private final MissionRepository missionRepository;
    private final DroneRepository droneRepository;
    private final WorkflowClient workflowClient;
    private final TemporalConfig temporalConfig;

    public MissionService(MissionRepository missionRepository,
                          DroneRepository droneRepository,
                          WorkflowClient workflowClient,
                          TemporalConfig temporalConfig) {
        this.missionRepository = missionRepository;
        this.droneRepository = droneRepository;
        this.workflowClient = workflowClient;
        this.temporalConfig = temporalConfig;
    }

    public List<MissionDTO> getAllMissions() {
        return missionRepository.findAll().stream()
                .map(MissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public MissionDTO getMissionById(Long id) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));
        return MissionDTO.fromEntity(mission);
    }

    public List<MissionDTO> getMissionsByStatus(Mission.Status status) {
        return missionRepository.findByStatus(status).stream()
                .map(MissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MissionDTO createMission(MissionCreateDTO dto) {
        Drone drone = null;

        if (dto.getDroneId() != null) {
            drone = droneRepository.findById(dto.getDroneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Drone not found with id: " + dto.getDroneId()));
            if (drone.getStatus() != Drone.Status.AVAILABLE) {
                throw new BadRequestException("Drone " + drone.getName() + " is currently " + drone.getStatus());
            }
        } else {
            List<Drone> available = droneRepository.findByStatus(Drone.Status.AVAILABLE);
            if (!available.isEmpty()) {
                drone = available.get(0);
            }
        }

        LocalDateTime scheduledTime = dto.getScheduledTime() != null ? dto.getScheduledTime() : LocalDateTime.now();

        Mission mission = new Mission(
                drone,
                dto.getName(),
                dto.getDestinationLat(),
                dto.getDestinationLng(),
                Mission.Status.SCHEDULED,
                scheduledTime
        );

        return MissionDTO.fromEntity(missionRepository.save(mission));
    }

    @Transactional
    public MissionDTO updateMissionStatus(Long id, Mission.Status status) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));
        mission.setStatus(status);

        if (mission.getDrone() != null) {
            if (status == Mission.Status.RUNNING) {
                mission.getDrone().setStatus(Drone.Status.IN_MISSION);
            } else if (status == Mission.Status.COMPLETED || status == Mission.Status.FAILED || status == Mission.Status.CANCELLED) {
                mission.getDrone().setStatus(Drone.Status.AVAILABLE);
            }
        }

        return MissionDTO.fromEntity(missionRepository.save(mission));
    }

    /**
     * Dispatches an autonomous mission into the Temporal.io Workflow Engine.
     */
    public MissionDTO launchMissionWorkflow(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));

        if (mission.getDrone() == null) {
            List<Drone> available = droneRepository.findByStatus(Drone.Status.AVAILABLE);
            if (available.isEmpty()) {
                throw new BadRequestException("No available drones in the fleet to execute mission");
            }
            mission.setDrone(available.get(0));
            missionRepository.save(mission);
        }

        String workflowId = "drone-mission-" + mission.getId();

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(temporalConfig.getTaskQueue())
                .build();

        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, options);

        log.info("Dispatching Temporal workflow '{}' for Mission #{}", workflowId, missionId);
        WorkflowClient.start(
                workflow::executeMission,
                mission.getId(),
                mission.getDrone().getId(),
                mission.getDestinationLat(),
                mission.getDestinationLng()
        );

        mission.setStatus(Mission.Status.RUNNING);
        return MissionDTO.fromEntity(missionRepository.save(mission));
    }

    /**
     * Sends an emergency abort signal to the running Temporal workflow, triggering Saga compensation.
     */
    public void abortMissionWorkflow(Long missionId, String reason) {
        String workflowId = "drone-mission-" + missionId;
        log.warn("Signaling emergency abort to Temporal workflow '{}', reason: {}", workflowId, reason);

        try {
            DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, workflowId);
            workflow.signalEmergencyAbort(reason);
        } catch (Exception e) {
            log.error("Failed to signal abort to workflow {}: {}", workflowId, e.getMessage());
            updateMissionStatus(missionId, Mission.Status.CANCELLED);
        }
    }

    /**
     * Queries real-time state from Temporal workflow without modifying state history.
     */
    public Map<String, Object> getWorkflowState(Long missionId) {
        String workflowId = "drone-mission-" + missionId;
        Map<String, Object> state = new HashMap<>();
        try {
            DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, workflowId);
            state.put("workflowId", workflowId);
            state.put("currentPhase", workflow.queryCurrentPhase());
            state.put("currentStep", workflow.queryCurrentStep());
            state.put("isAborted", workflow.isAborted());
            state.put("active", true);
        } catch (Exception e) {
            state.put("workflowId", workflowId);
            state.put("active", false);
            state.put("message", "Workflow not active or already closed: " + e.getMessage());
        }
        return state;
    }
}
