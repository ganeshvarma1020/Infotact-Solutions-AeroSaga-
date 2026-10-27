package com.infotact.aerosaga.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.domain.MissionPlanValidator;
import com.infotact.aerosaga.domain.OperatingLimits;
import com.infotact.aerosaga.fleet.DroneFleet;
import com.infotact.aerosaga.fleet.FlightSimulator;
import com.infotact.aerosaga.persistence.SimulationStateStore;
import com.infotact.aerosaga.workflow.MissionActivities;
import com.infotact.aerosaga.workflow.MissionActivitiesImpl;
import com.infotact.aerosaga.workflow.MissionJournal;

/** Creates the plain-Java core objects once and shares them across the app. */
@Configuration
public class CoreConfig {

    @Bean
    OperatingLimits operatingLimits() {
        return OperatingLimits.bengaluruDemo();
    }

    @Bean
    MissionPlanValidator missionPlanValidator(OperatingLimits limits) {
        return new MissionPlanValidator(limits);
    }

    @Bean
    DroneFleet droneFleet(OperatingLimits limits) {
        return new DroneFleet(limits.operatingCenter(), DroneFleet.demoSpecs());
    }

    @Bean
    AirspaceRegistry airspaceRegistry(AeroSagaProperties props) {
        return new AirspaceRegistry(props.airspace().capacity());
    }

    @Bean
    FlightSimulator flightSimulator(DroneFleet fleet, AirspaceRegistry airspace, OperatingLimits limits) {
        return new FlightSimulator(fleet, airspace, limits);
    }

    @Bean
    MissionActivities missionActivities(MissionPlanValidator validator, OperatingLimits limits, DroneFleet fleet,
                                        AirspaceRegistry airspace, FlightSimulator simulator, MissionJournal journal,
                                        SimulationStateStore stateStore) {
        return new MissionActivitiesImpl(validator, limits, fleet, airspace, simulator, journal, stateStore::checkpoint);
    }
}
