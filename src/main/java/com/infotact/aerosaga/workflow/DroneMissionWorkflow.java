package com.infotact.aerosaga.workflow;

import com.infotact.aerosaga.model.*;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;

/**
 * Temporal Workflow contract orchestrating the end-to-end autonomous drone mission lifecycle.
 */
@WorkflowInterface
public interface DroneMissionWorkflow {

    /**
     * Main entry point for the long-running autonomous mission.
     */
    @WorkflowMethod
    MissionResult executeMission(MissionRequest request);

    /**
     * Signal to dynamically update flight plan with new waypoints during flight.
     */
    @SignalMethod
    void signalReroute(RerouteRequest reroute);

    /**
     * Signal to command the drone to pause and hover in place for specified seconds.
     */
    @SignalMethod
    void signalPauseHover(int seconds);

    /**
     * Signal to resume normal transit along active flight plan.
     */
    @SignalMethod
    void signalResume();

    /**
     * Signal to immediately trigger fail-safe abort and execute the Saga compensation rollback.
     */
    @SignalMethod
    void signalEmergencyAbort(String reason);

    /**
     * Query to inspect real-time mission status without modifying workflow history.
     */
    @QueryMethod
    MissionStatus queryMissionStatus();

    /**
     * Query to fetch latest real-time avionics telemetry.
     */
    @QueryMethod
    DroneTelemetry queryCurrentTelemetry();

    /**
     * Query to fetch current operational phase description.
     */
    @QueryMethod
    String queryCurrentPhase();

    /**
     * Query active waypoint trajectory list.
     */
    @QueryMethod
    List<Waypoint> queryActiveWaypoints();
}
