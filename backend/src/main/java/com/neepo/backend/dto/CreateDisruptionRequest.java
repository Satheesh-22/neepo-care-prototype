package com.neepo.backend.dto;

import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateDisruptionRequest(
        @NotNull UUID eventId,
        @NotNull UUID tripId,
        @NotNull UUID contextSnapshotId,
        @NotNull DisruptionType type,
        @NotNull DisruptionSeverity severity,
        @NotNull Instant detectedAt,
        List<String> explanationCodes,
        @NotNull Map<String, Object> evidence
) {
    public CreateDisruptionRequest {
        explanationCodes = explanationCodes == null ? List.of() : List.copyOf(explanationCodes);
        evidence = evidence == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(evidence));
    }
}
