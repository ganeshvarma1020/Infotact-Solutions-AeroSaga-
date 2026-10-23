package com.infotact.aerosaga.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MissionActivities {

    @ActivityMethod
    boolean validateMission(Long missionId);

    @ActivityMethod
    boolean reserveDrone(Long missionId, Long droneId);

    @ActivityMethod
    boolean simulateFlightStep(Long missionId, Long droneId, int step, int totalSteps, double destLat, double destLng);

    @ActivityMethod
    boolean finalizeMission(Long missionId, Long droneId);

    @ActivityMethod
    void compensateMissionFailure(Long missionId, Long droneId, String reason);
}
