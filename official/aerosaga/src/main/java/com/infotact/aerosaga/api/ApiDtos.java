package com.infotact.aerosaga.api;

import java.time.Instant;
import java.util.List;

import com.infotact.aerosaga.domain.FailureScenario;
import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.NoFlyZone;
import com.infotact.aerosaga.fleet.DroneSnapshot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON shapes used by the REST API. Kept separate from domain classes so the
 * API can stay stable while internals change.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    /** POST /api/missions body. Shape checks here; business rules in MissionPlanValidator. */
    public record CreateMissionRequest(
            @NotBlank @Size(max = 100) String operator,
            @NotEmpty @Size(max = 10) List<@Valid WaypointInput> waypoints,
            @NotNull @DecimalMin("0.0") @DecimalMax("50.0") Double payloadKg,
            FailureScenario scenario) {
    }

    public record WaypointInput(
            @NotBlank @Size(max = 60) String name,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lon) {
    }

    public record AbortRequest(@Size(max = 200) String reason) {
    }

    public record MissionSummary(
            String id, String operator, String phase, String scenario, String droneId,
            double payloadKg, double routeKm, String failureCode, Instant createdAt, Instant finishedAt) {
    }

    public record EventView(String phase, String message, Instant occurredAt) {
    }

    public record FleetView(List<DroneSnapshot> drones, int slotsInUse, int slotCapacity, List<NoFlyZone> temporaryZones) {
    }

    public record ConfigView(GeoPoint base, GeoPoint operatingCenter, double operatingRadiusKm,
                             double maxPayloadKg, int maxWaypoints, double maxRouteKm, List<NoFlyZone> noFlyZones) {
    }
}
