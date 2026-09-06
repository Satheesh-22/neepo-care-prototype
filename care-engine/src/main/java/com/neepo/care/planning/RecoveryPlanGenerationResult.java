package com.neepo.care.planning;

import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record RecoveryPlanGenerationResult(
        List<RecoveryPlan> plans,
        Map<RecoveryPlanType, String> skippedTemplates
) {
    public RecoveryPlanGenerationResult {
        plans = List.copyOf(plans);
        EnumMap<RecoveryPlanType, String> orderedReasons = new EnumMap<>(RecoveryPlanType.class);
        orderedReasons.putAll(skippedTemplates);
        skippedTemplates = Collections.unmodifiableMap(orderedReasons);
    }

    public long normalPlanCount() {
        return plans.stream()
                .filter(plan -> plan.type() != RecoveryPlanType.EMERGENCY_ESCALATION)
                .count();
    }
}
