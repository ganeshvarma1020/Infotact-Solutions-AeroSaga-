package com.infotact.aerosaga.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The public contract of one mission.
 * - execute: runs the whole mission, start to finish (can take minutes or hours).
 * - abort:   a signal. Fire-and-forget message from the operator into the running mission.
 * - status:  a query. Read the live state without changing anything.
 */
@WorkflowInterface
public interface MissionWorkflow {

    @WorkflowMethod
    MissionResult execute(MissionInput input);

    @SignalMethod
    void abort(String reason);

    @QueryMethod
    MissionView status();
}
