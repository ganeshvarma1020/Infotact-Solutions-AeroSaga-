package com.infotact.aerosaga.exception;

public class InsufficientBatteryException extends RuntimeException {
    public InsufficientBatteryException(String message) {
        super(message);
    }
}
