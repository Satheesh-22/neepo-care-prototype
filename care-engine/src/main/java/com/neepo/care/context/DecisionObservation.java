package com.neepo.care.context;

import com.neepo.contracts.ObservationMetadata;

public record DecisionObservation(
        String key,
        ObservationRequirement requirement,
        ObservationMetadata metadata
) {
    public DecisionObservation {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Observation key is required");
        }
        if (requirement == null) {
            throw new IllegalArgumentException("Observation requirement is required");
        }
    }
}
