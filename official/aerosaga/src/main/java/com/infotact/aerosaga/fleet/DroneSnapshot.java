package com.infotact.aerosaga.fleet;

import com.infotact.aerosaga.domain.GeoPoint;

/** Read-only copy of a drone's state at one moment. Safe to hand out. */
public record DroneSnapshot(
        String droneId,
        String model,
        DroneStatus status,
        double batteryPct,
        double payloadKg,
        GeoPoint position,
        String missionId) {
}
