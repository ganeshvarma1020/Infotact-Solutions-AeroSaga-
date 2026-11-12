package com.infotact.aerosaga.activity;

import com.infotact.aerosaga.model.DroneTelemetry;
import com.infotact.aerosaga.model.Waypoint;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface FlightControlActivities {

    @ActivityMethod
    void initiateTakeoff(String droneId, String missionId, double targetAltitudeMeters);

    @ActivityMethod
    DroneTelemetry flyToWaypoint(String droneId, String missionId, Waypoint waypoint, int waypointIndex, int totalWaypoints);

    @ActivityMethod
    void hoverInPlace(String droneId, String missionId, int seconds);

    @ActivityMethod
    void returnToBase(String droneId, String missionId, Waypoint baseWaypoint);

    @ActivityMethod
    void initiateLanding(String droneId, String missionId);
}
