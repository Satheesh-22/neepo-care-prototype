package com.neepo.contracts;

import java.time.Instant;

public record ObservationMetadata(
        Instant observedAt,
        Instant receivedAt,
        Instant expiresAt,
        double confidence,
        ObservationStatus status,
        String source
) {
}
