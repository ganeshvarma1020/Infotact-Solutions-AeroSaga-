package com.infotact.aerosaga.domain;

import java.util.List;

/**
 * Hard limits for every mission. One place to change the rules.
 *
 * @param operatingCenter    center of the area we are allowed to fly in
 * @param operatingRadiusKm  every waypoint must be within this distance of the center
 * @param maxWaypoints       upper bound on stops per mission
 * @param maxRouteKm         upper bound on total route length (including return)
 * @param maxPayloadKg       heaviest payload any drone in the fleet can lift
 * @param batteryReservePct  battery that must remain on landing; never planned away
 * @param noFlyZones         permanent restricted areas
 */
public record OperatingLimits(
        GeoPoint operatingCenter,
        double operatingRadiusKm,
        int maxWaypoints,
        double maxRouteKm,
        double maxPayloadKg,
        double batteryReservePct,
        List<NoFlyZone> noFlyZones) {

    public OperatingLimits {
        noFlyZones = noFlyZones == null ? List.of() : List.copyOf(noFlyZones);
    }

    /** Defaults for the demo: Electronic City, Bengaluru, with one restricted zone. */
    public static OperatingLimits bengaluruDemo() {
        return new OperatingLimits(
                new GeoPoint(12.8456, 77.6603),
                25.0,
                10,
                40.0,
                5.0,
                20.0,
                List.of(new NoFlyZone("HAL Airport", new GeoPoint(12.9500, 77.6682), 3.0)));
    }
}
