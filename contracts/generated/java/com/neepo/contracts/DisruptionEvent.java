package com.neepo.contracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DisruptionEvent(
        UUID id,
        UUID eventId,
        UUID tripId,
        UUID contextSnapshotId,
        DisruptionType type,
        DisruptionSeverity severity,
        Instant detectedAt,
        boolean active,
        List<String> explanationCodes,
        Map<String, Object> evidence
) {
}
