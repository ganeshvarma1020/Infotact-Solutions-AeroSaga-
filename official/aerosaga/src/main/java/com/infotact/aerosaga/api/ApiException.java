package com.infotact.aerosaga.api;

import java.util.List;

import org.springframework.http.HttpStatus;

/** An error that maps straight to an HTTP status, with optional detail lines. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> errors;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<String> errors) {
        super(message);
        this.status = status;
        this.errors = List.copyOf(errors);
    }

    public HttpStatus status() {
        return status;
    }

    public List<String> errors() {
        return errors;
    }
}
