package com.neepo.care.orchestration;

import com.neepo.care.context.ContextFreshnessPolicy;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.evaluation.HardConstraintEvaluationPolicy;
import com.neepo.care.evaluation.HardConstraintEvaluationResult;
import com.neepo.care.evaluation.HardConstraintEvaluator;
import com.neepo.care.planning.RecoveryPlanGenerationPolicy;
import com.neepo.care.planning.RecoveryPlanGenerationResult;
import com.neepo.care.planning.RecoveryPlanGenerator;
import com.neepo.care.scoring.RecoveryPlanScorer;
import com.neepo.care.scoring.RecoveryPlanScoringResult;
import com.neepo.care.scoring.SoftConstraintScoringPolicy;
import com.neepo.care.selection.RecoveryPlanSelectionResult;
import com.neepo.care.selection.RecoveryPlanSelector;
import com.neepo.contracts.TripContext;

import java.util.Objects;
import java.util.UUID;

public final class RecoveryDecisionEngine {
    private final RecoveryPlanGenerator generator;
    private final HardConstraintEvaluator hardEvaluator;
    private final RecoveryPlanScorer scorer;
    private final RecoveryPlanSelector selector;

    public RecoveryDecisionEngine(
            RecoveryPlanGenerator generator,
            HardConstraintEvaluator hardEvaluator,
            RecoveryPlanScorer scorer,
            RecoveryPlanSelector selector
    ) {
        this.generator = Objects.requireNonNull(generator, "Recovery plan generator is required");
        this.hardEvaluator = Objects.requireNonNull(hardEvaluator, "Hard constraint evaluator is required");
        this.scorer = Objects.requireNonNull(scorer, "Recovery plan scorer is required");
        this.selector = Objects.requireNonNull(selector, "Recovery plan selector is required");
    }

    public static RecoveryDecisionEngine demoDefaults() {
        return new RecoveryDecisionEngine(
                new RecoveryPlanGenerator(RecoveryPlanGenerationPolicy.demoDefaults()),
                new HardConstraintEvaluator(
                        new ContextFreshnessValidator(ContextFreshnessPolicy.defaults()),
                        HardConstraintEvaluationPolicy.demoDefaults()
                ),
                new RecoveryPlanScorer(SoftConstraintScoringPolicy.demoDefaults()),
                new RecoveryPlanSelector()
        );
    }

    public RecoveryDecisionResult decide(
            TripContext context,
            UUID disruptionId,
            DetectedDisruption disruption
    ) {
        return decide(context, disruptionId, disruption, false);
    }

    RecoveryDecisionResult redecide(
            TripContext context,
            UUID disruptionId,
            DetectedDisruption activeDisruption
    ) {
        return decide(context, disruptionId, activeDisruption, true);
    }

    private RecoveryDecisionResult decide(
            TripContext context,
            UUID disruptionId,
            DetectedDisruption disruption,
            boolean replanning
    ) {
        Objects.requireNonNull(context, "Trip context is required");
        Objects.requireNonNull(disruptionId, "Disruption id is required");
        Objects.requireNonNull(disruption, "Detected disruption is required");

        RecoveryPlanGenerationResult generation = replanning
                ? generator.regenerate(context, disruptionId, disruption)
                : generator.generate(context, disruptionId, disruption);
        HardConstraintEvaluationResult hardEvaluation = hardEvaluator.evaluate(context, generation.plans());
        RecoveryPlanScoringResult scoring = scorer.score(context, hardEvaluation);
        RecoveryPlanSelectionResult selection = selector.select(scoring);
        return new RecoveryDecisionResult(generation, hardEvaluation, scoring, selection);
    }
}
