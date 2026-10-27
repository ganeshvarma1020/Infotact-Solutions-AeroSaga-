package com.infotact.aerosaga.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions into clean JSON errors (RFC 9457 "problem details").
 * Never leaks stack traces to the caller; logs them on the server instead.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail api(ApiException e) {
        return problem(e.status(), e.getMessage(), e.errors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return problem(HttpStatus.BAD_REQUEST, "Request body is invalid", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Request body is not valid JSON or has a wrong field type", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail illegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail illegalState(IllegalStateException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }

    private static ProblemDetail problem(HttpStatus status, String detail, List<String> errors) {
        if (status.is5xxServerError()) {
            log.error("API error {}: {}", status.value(), detail);
        }
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        if (!errors.isEmpty()) {
            p.setProperty("errors", errors);
        }
        return p;
    }
}
