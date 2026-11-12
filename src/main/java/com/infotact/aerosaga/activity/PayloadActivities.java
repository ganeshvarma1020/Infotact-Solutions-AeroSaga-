package com.infotact.aerosaga.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PayloadActivities {

    @ActivityMethod
    boolean deployDeliveryPayload(String droneId, String missionId, double payloadWeightKg);

    @ActivityMethod
    boolean executeSurveillanceScan(String droneId, String missionId, String targetLabel);
}
