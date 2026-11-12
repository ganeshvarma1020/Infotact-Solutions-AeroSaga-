package com.infotact.aerosaga.exception;

public class MissionExecutionException extends RuntimeException {
    public MissionExecutionException(String message) {
        super(message);
    }

    public MissionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
