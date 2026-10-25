package com.infotact.aerosaga;

import com.infotact.aerosaga.dto.MissionCreateDTO;
import com.infotact.aerosaga.dto.MissionDTO;
import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.Mission;
import com.infotact.aerosaga.model.MissionEvent;
import com.infotact.aerosaga.model.Telemetry;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.repository.MissionEventRepository;
import com.infotact.aerosaga.repository.MissionRepository;
import com.infotact.aerosaga.repository.TelemetryRepository;
import com.infotact.aerosaga.service.MissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AeroSagaTemporalWorkflowTests {

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private DroneRepository droneRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private MissionEventRepository missionEventRepository;

    @Test
    @DisplayName("Should successfully execute deterministic DroneMissionWorkflow via Temporal engine")
    void testAutonomousMissionWorkflowExecution() throws Exception {
        // 1. Arrange: Create a mission
        MissionCreateDTO createDTO = new MissionCreateDTO(
                null,
                "Temporal Recon Mission",
                12.9850,
                77.6100,
                LocalDateTime.now()
        );
        MissionDTO mission = missionService.createMission(createDTO);
        assertThat(mission.getId()).isNotNull();

        // 2. Act: Launch workflow in Temporal engine
        MissionDTO launched = missionService.launchMissionWorkflow(mission.getId());
        assertThat(launched.getStatus()).isEqualTo(Mission.Status.RUNNING);

        // 3. Await workflow completion (5 steps x 800ms ~ 4 seconds)
        long startTime = System.currentTimeMillis();
        boolean completed = false;
        while (System.currentTimeMillis() - startTime < 15000) {
            Mission current = missionRepository.findById(mission.getId()).orElseThrow();
            if (current.getStatus() == Mission.Status.COMPLETED) {
                completed = true;
                break;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }

        // 4. Assert: Workflow completed, drone returned to AVAILABLE, telemetry & events recorded
        assertThat(completed).isTrue();

        Mission finishedMission = missionRepository.findById(mission.getId()).orElseThrow();
        assertThat(finishedMission.getStatus()).isEqualTo(Mission.Status.COMPLETED);

        Drone drone = finishedMission.getDrone();
        assertThat(drone).isNotNull();
        assertThat(drone.getStatus()).isEqualTo(Drone.Status.AVAILABLE);

        List<Telemetry> telemetryList = telemetryRepository.findByMissionIdOrderByTimestampAsc(mission.getId());
        assertThat(telemetryList).isNotEmpty();
        assertThat(telemetryList.size()).isGreaterThanOrEqualTo(5);

        List<MissionEvent> events = missionEventRepository.findByMissionIdOrderByTimestampAsc(mission.getId());
        assertThat(events).isNotEmpty();
        assertThat(events.stream().anyMatch(e -> e.getEventType().equals("VALIDATION_PASSED"))).isTrue();
        assertThat(events.stream().anyMatch(e -> e.getEventType().equals("TAKEOFF"))).isTrue();
        assertThat(events.stream().anyMatch(e -> e.getEventType().equals("WAYPOINT_REACHED"))).isTrue();
        assertThat(events.stream().anyMatch(e -> e.getEventType().equals("COMPLETED"))).isTrue();
    }

    @Test
    @DisplayName("Should execute Saga compensation rollback upon emergency abort signal")
    void testTemporalSagaCompensationOnAbort() throws Exception {
        // 1. Arrange
        MissionCreateDTO createDTO = new MissionCreateDTO(
                null,
                "Emergency Abort Test Mission",
                12.9900,
                77.6200,
                LocalDateTime.now()
        );
        MissionDTO mission = missionService.createMission(createDTO);

        // 2. Act: Launch
        missionService.launchMissionWorkflow(mission.getId());

        // Wait briefly for takeoff
        TimeUnit.MILLISECONDS.sleep(600);

        // Trigger Emergency Abort
        missionService.abortMissionWorkflow(mission.getId(), "Severe atmospheric storm alert");

        // 3. Await compensation completion
        long startTime = System.currentTimeMillis();
        boolean compensated = false;
        while (System.currentTimeMillis() - startTime < 10000) {
            Mission current = missionRepository.findById(mission.getId()).orElseThrow();
            if (current.getStatus() == Mission.Status.FAILED) {
                compensated = true;
                break;
            }
            TimeUnit.MILLISECONDS.sleep(300);
        }

        // 4. Assert Saga compensation executed
        assertThat(compensated).isTrue();

        List<MissionEvent> events = missionEventRepository.findByMissionIdOrderByTimestampAsc(mission.getId());
        assertThat(events.stream().anyMatch(e -> e.getEventType().equals("ERROR"))).isTrue();
    }
}
