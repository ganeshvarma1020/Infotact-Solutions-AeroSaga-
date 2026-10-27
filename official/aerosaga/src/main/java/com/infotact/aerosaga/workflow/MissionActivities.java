package com.infotact.aerosaga.workflow;

import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.MissionRequest;
import com.infotact.aerosaga.domain.RoutePlan;
import com.infotact.aerosaga.fleet.DroneSnapshot;
import com.infotact.aerosaga.fleet.LegTelemetry;
import io.temporal.activity.ActivityInterface;

/**
 * Every action that touches the outside world (fleet, airspace, database).
 * Workflow code calls these through a Temporal stub, so each call is recorded,
 * retried on failure, and never repeated after it has succeeded.
 *
 * Each "do" action has an "undo" partner used by the saga:
 *   reserveAirspace -> releaseAirspace
 *   assignDrone     -> releaseDrone
 *   loadPayload     -> unloadPayload
 *   flyLeg          -> returnToHome
 * Every undo is idempotent and safe to call even if the "do" never happened.
 */
@ActivityInterface
public interface MissionActivities {

    /** Validate the plan and return its legs. Fails (no retry) if any rule is broken. */
    RoutePlan planRoute(MissionRequest request);

    String reserveAirspace(String missionId);

    void releaseAirspace(String missionId);

    DroneSnapshot assignDrone(String missionId, double payloadKg, double routeKm);

    void releaseDrone(String missionId);

    void loadPayload(String missionId, double payloadKg);

    void unloadPayload(String missionId);

    LegTelemetry flyLeg(String missionId, RoutePlan.Leg leg, double payloadKg, FailureScenario scenario);

    void deliverPayload(String missionId);

    LegTelemetry returnToHome(String missionId);

    void recordEvent(MissionEvent event);

    void recordOutcome(MissionResult result);
}
