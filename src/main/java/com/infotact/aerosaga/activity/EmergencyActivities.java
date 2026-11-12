package com.infotact.aerosaga.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Compensating transactions invoked by the Temporal Saga when mission failure or emergency abort occurs.
 */
@ActivityInterface
public interface EmergencyActivities {

    @ActivityMethod
    void executeEmergencySafeTouchdown(String droneId, String missionId, String failureReason);

    @ActivityMethod
    void securePayloadLockdown(String droneId, String missionId);

    @ActivityMethod
    void broadcastAirspaceEmergencyBeacon(String droneId, String missionId, String emergencyReason);

    @ActivityMethod
    void recordMissionCompensation(String missionId, String compensationDetails);
}
