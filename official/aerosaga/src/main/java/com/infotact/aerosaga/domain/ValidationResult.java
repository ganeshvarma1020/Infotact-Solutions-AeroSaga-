package com.infotact.aerosaga.domain;

import java.util.List;

/** Outcome of validating a mission. Collects every problem, not just the first. */
public record ValidationResult(List<String> errors) {

    public ValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
