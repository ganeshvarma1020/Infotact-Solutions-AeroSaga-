package com.infotact.aerosaga.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DroneMissionWorkflow {

    @WorkflowMethod
    String executeMission(Long missionId, Long droneId, double destLat, double destLng);

    @SignalMethod
    void signalEmergencyAbort(String reason);

    @SignalMethod
    void signalPause(int seconds);

    @SignalMethod
    void signalResume();

    @QueryMethod
    String queryCurrentPhase();

    @QueryMethod
    int queryCurrentStep();

    @QueryMethod
    boolean isAborted();
}
