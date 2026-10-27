package com.infotact.aerosaga.domain;

/**
 * A business failure with a stable machine-readable code.
 * The Temporal layer maps these codes to non-retryable failures,
 * because retrying "no drone available" or "low battery" will not fix them.
 */
public class MissionException extends RuntimeException {

    public enum Code {
        INVALID_MISSION,
        NO_DRONE_AVAILABLE,
        AIRSPACE_FULL,
        LOW_BATTERY,
        GEOFENCE_BREACH,
        TELEMETRY_LOST
    }

    private final Code code;

    public MissionException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
