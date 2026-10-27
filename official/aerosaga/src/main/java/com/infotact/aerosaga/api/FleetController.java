package com.infotact.aerosaga.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.api.ApiDtos.ConfigView;
import com.infotact.aerosaga.api.ApiDtos.FleetView;
import com.infotact.aerosaga.domain.OperatingLimits;
import com.infotact.aerosaga.fleet.DroneFleet;
import com.infotact.aerosaga.persistence.SimulationStateStore;

/** Fleet, airspace and settings for the dashboard. */
@RestController
public class FleetController {

    private final DroneFleet fleet;
    private final AirspaceRegistry airspace;
    private final OperatingLimits limits;
    private final SimulationStateStore stateStore;

    public FleetController(DroneFleet fleet, AirspaceRegistry airspace, OperatingLimits limits,
                           SimulationStateStore stateStore) {
        this.fleet = fleet;
        this.airspace = airspace;
        this.limits = limits;
        this.stateStore = stateStore;
    }

    @GetMapping("/api/fleet")
    public FleetView fleet() {
        return new FleetView(fleet.list(), airspace.inUse(), airspace.capacity(), airspace.temporaryZones());
    }

    /** Swap in a full battery. Only allowed while the drone is on the ground (409 otherwise). */
    @PostMapping("/api/fleet/{droneId}/recharge")
    public ResponseEntity<Void> recharge(@PathVariable String droneId) {
        if (fleet.find(droneId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Drone " + droneId + " not found");
        }
        fleet.recharge(droneId);
        stateStore.checkpoint();
        return ResponseEntity.noContent().build();
    }

    /** Lift a temporary no-fly zone. */
    @DeleteMapping("/api/airspace/zones/{name}")
    public ResponseEntity<Void> clearZone(@PathVariable String name) {
        airspace.clearTemporaryZone(name);
        stateStore.checkpoint();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/config")
    public ConfigView config() {
        return new ConfigView(fleet.home(), limits.operatingCenter(), limits.operatingRadiusKm(), limits.maxPayloadKg(),
                limits.maxWaypoints(), limits.maxRouteKm(), limits.noFlyZones());
    }

    /**
     * Hands the browser its CSRF token. The dashboard sends it back in the X-XSRF-TOKEN header
     * on every POST/DELETE, which proves the request came from our own page.
     */
    @GetMapping("/api/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", "X-XSRF-TOKEN", "token", token.getToken());
    }
}
