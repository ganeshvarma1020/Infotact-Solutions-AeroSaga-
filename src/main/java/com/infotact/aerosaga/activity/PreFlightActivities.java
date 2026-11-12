package com.infotact.aerosaga.activity;

import com.infotact.aerosaga.model.Waypoint;
import com.infotact.aerosaga.model.WeatherReport;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

@ActivityInterface
public interface PreFlightActivities {

    @ActivityMethod
    boolean verifyBattery(String droneId, double requiredMinimumPercent);

    @ActivityMethod
    WeatherReport evaluateAirspaceWeather(double latitude, double longitude);

    @ActivityMethod
    boolean verifyAirspaceClearance(List<Waypoint> waypoints);

    @ActivityMethod
    boolean calibrateSensorsAndMotors(String droneId);
}
