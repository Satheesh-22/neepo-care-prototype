package com.neepo.contracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ActiveDisruption(
        UUID disruptionId,
        UUID eventId,
        DisruptionType type,
        DisruptionSeverity severity,
        Instant detectedAt,
        List<String> explanationCodes,
        Map<String, Object> evidence
) {
}
