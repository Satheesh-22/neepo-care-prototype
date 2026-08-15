package com.neepo.contracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DecisionExplanation(
        UUID tripId,
        UUID disruptionId,
        UUID selectedPlanId,
        List<UUID> rejectedPlanIds,
        List<String> selectionExplanationCodes,
        Map<UUID, List<String>> rejectionExplanationCodes,
        String deterministicTieBreakRule,
        Instant explainedAt
) {
}
