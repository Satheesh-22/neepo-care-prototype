package com.neepo.care.orchestration;

import com.neepo.care.evaluation.HardConstraintEvaluationResult;
import com.neepo.care.planning.RecoveryPlanGenerationResult;
import com.neepo.care.scoring.RecoveryPlanScoringResult;
import com.neepo.care.selection.RecoveryPlanSelectionResult;

import java.util.Objects;

public record RecoveryDecisionResult(
        RecoveryPlanGenerationResult generation,
        HardConstraintEvaluationResult hardEvaluation,
        RecoveryPlanScoringResult scoring,
        RecoveryPlanSelectionResult selection
) {
    public RecoveryDecisionResult {
        generation = Objects.requireNonNull(generation, "Plan generation result is required");
        hardEvaluation = Objects.requireNonNull(hardEvaluation, "Hard evaluation result is required");
        scoring = Objects.requireNonNull(scoring, "Plan scoring result is required");
        selection = Objects.requireNonNull(selection, "Plan selection result is required");
        int generatedCount = generation.plans().size();
        if (hardEvaluation.planEvaluations().size() != generatedCount
                || scoring.plans().size() != generatedCount
                || selection.plans().size() != generatedCount) {
            throw new IllegalArgumentException("Every decision stage must retain the complete candidate set");
        }
    }
}
