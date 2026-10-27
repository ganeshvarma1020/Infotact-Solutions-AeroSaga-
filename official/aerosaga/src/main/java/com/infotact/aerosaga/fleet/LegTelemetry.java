package com.infotact.aerosaga.fleet;

import com.infotact.aerosaga.domain.GeoPoint;

/** What the drone reports after finishing a leg. */
public record LegTelemetry(
        int legIndex,
        String reachedName,
        GeoPoint position,
        double distanceKm,
        double batteryPct,
        long flightSeconds) {
}
