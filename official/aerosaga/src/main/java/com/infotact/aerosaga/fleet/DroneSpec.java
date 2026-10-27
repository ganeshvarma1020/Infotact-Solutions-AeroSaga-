package com.infotact.aerosaga.fleet;

/**
 * Fixed facts about a drone airframe.
 *
 * @param rangeKm        distance on a full battery with no payload
 * @param maxPayloadKg   heaviest load it can carry
 * @param cruiseSpeedKmh used to estimate flight time per leg
 */
public record DroneSpec(String droneId, String model, double maxPayloadKg, double rangeKm, double cruiseSpeedKmh) {

    public DroneSpec {
        if (droneId == null || droneId.isBlank()) {
            throw new IllegalArgumentException("droneId is required");
        }
        if (!(maxPayloadKg > 0) || !(rangeKm > 0) || !(cruiseSpeedKmh > 0)) {
            throw new IllegalArgumentException("payload, range and speed must be positive");
        }
    }
}
