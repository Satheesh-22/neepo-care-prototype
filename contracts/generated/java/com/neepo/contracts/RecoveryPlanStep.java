package com.neepo.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecoveryPlanStep(
        int sequence,
        String actionCode,
        String instruction,
        UUID providerId,
        Instant expectedStartAt,
        Instant expectedCompleteAt,
        List<String> requiredResources
) {
}
