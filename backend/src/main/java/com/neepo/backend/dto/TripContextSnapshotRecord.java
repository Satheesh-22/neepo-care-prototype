package com.neepo.backend.dto;

import com.neepo.contracts.NetworkState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TripContextSnapshotRecord(
        UUID id,
        UUID tripId,
        long snapshotSequence,
        Instant capturedAt,
        BigDecimal minimumConfidence,
        boolean hasExpiredRequiredData,
        NetworkState networkState,
        String contextPayload,
        String contextHash
) {
    public TripContextSnapshotRecord {
        Objects.requireNonNull(id, "Context snapshot id is required");
        Objects.requireNonNull(tripId, "Context snapshot trip id is required");
        Objects.requireNonNull(capturedAt, "Context snapshot capture time is required");
        Objects.requireNonNull(minimumConfidence, "Context snapshot confidence is required");
        Objects.requireNonNull(networkState, "Context snapshot network state is required");
        if (snapshotSequence <= 0) {
            throw new IllegalArgumentException("Context snapshot sequence must be positive");
        }
        if (minimumConfidence.signum() < 0 || minimumConfidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Context snapshot confidence must be between zero and one");
        }
        if (contextPayload == null || contextPayload.isBlank()) {
            throw new IllegalArgumentException("Context snapshot payload is required");
        }
        if (contextHash == null || !contextHash.matches("[a-fA-F0-9]{64}")) {
            throw new IllegalArgumentException("Context snapshot hash must be SHA-256 hex");
        }
    }
}
