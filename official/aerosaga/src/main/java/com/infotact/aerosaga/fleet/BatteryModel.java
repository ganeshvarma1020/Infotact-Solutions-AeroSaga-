package com.infotact.aerosaga.fleet;

/**
 * Simple, explainable energy model.
 * Empty drone: 100% battery = spec.rangeKm.
 * Full payload: uses up to 50% more energy per km.
 */
public final class BatteryModel {

    static final double MAX_PAYLOAD_PENALTY = 0.5;

    private BatteryModel() {
    }

    /** Battery percent used to fly one km with this payload. */
    public static double percentPerKm(DroneSpec spec, double payloadKg) {
        double loadRatio = Math.max(0, Math.min(1, payloadKg / spec.maxPayloadKg()));
        return (100.0 / spec.rangeKm()) * (1 + MAX_PAYLOAD_PENALTY * loadRatio);
    }

    /** Battery percent needed for a whole route. */
    public static double percentFor(DroneSpec spec, double payloadKg, double km) {
        return percentPerKm(spec, payloadKg) * km;
    }
}
