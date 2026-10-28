package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ComplianceVerificationResponse(
        UUID manifestId,
        boolean valid,
        String expectedDigest,
        String computedDigest,
        Instant verifiedAt,
        String message
) {
}
