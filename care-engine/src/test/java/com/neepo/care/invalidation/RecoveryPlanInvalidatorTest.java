package com.neepo.care.invalidation;

import com.neepo.care.scoring.RecoveryPlanScorer;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.PlanInvalidationReason;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecoveryPlanInvalidatorTest {
    private static final Instant GENERATED_AT = Instant.parse("2026-08-23T11:55:00Z");
    private static final Instant SELECTED_AT = Instant.parse("2026-08-23T12:00:00Z");
    private static final Instant OBSERVED_AT = Instant.parse("2026-08-23T12:10:00Z");
    private static final UUID PLAN_ID = id("00000000-0000-4000-9100-000000000001");
    private static final UUID TRIP_ID = id("00000000-0000-4000-8600-000000000001");
    private static final UUID DISRUPTION_ID = id("00000000-0000-4000-9000-000000000001");
    private static final UUID PROVIDER_ID = id("00000000-0000-4000-8700-000000000001");
    private static final UUID EVENT_ID = id("10000000-0000-4000-9000-000000000003");

    private final RecoveryPlanInvalidator invalidator = new RecoveryPlanInvalidator();

    @Test
    void providerCancellationInvalidatesSelectedPlanAndRequestsReplanning() {
        RecoveryPlan original = activePlan(RecoveryPlanStatus.SELECTED, PROVIDER_ID);
        ConfirmedPlanChange change = change(PlanInvalidationReason.PROVIDER_CANCELLED);

        RecoveryPlanInvalidationResult result = invalidator.invalidate(original, change);

        assertEquals(RecoveryPlanStatus.INVALIDATED, result.invalidatedPlan().status());
        assertEquals(PLAN_ID, result.invalidation().planId());
        assertEquals(EVENT_ID, result.invalidation().eventId());
        assertEquals(PlanInvalidationReason.PROVIDER_CANCELLED, result.invalidation().reason());
        assertEquals(RecoveryPlanInvalidator.PROVIDER_CANCELLED, result.invalidation().explanationCode());
        assertEquals(OBSERVED_AT, result.invalidation().observedAt());
        assertEquals(TRIP_ID, result.replanningTrigger().tripId());
        assertEquals(DISRUPTION_ID, result.replanningTrigger().disruptionId());
        assertEquals(result.invalidation().id(), result.replanningTrigger().invalidationId());
        assertEquals(EVENT_ID, result.replanningTrigger().sourceEventId());
        assertEquals(OBSERVED_AT, result.replanningTrigger().requestedAt());
    }

    @Test
    void executingPlanCanBeInvalidatedWithoutLosingSelectionMetadata() {
        RecoveryPlan executing = activePlan(RecoveryPlanStatus.EXECUTING, PROVIDER_ID);

        RecoveryPlanInvalidationResult result = invalidator.invalidate(
                executing,
                change(PlanInvalidationReason.ROUTE_BLOCKED)
        );

        assertEquals(RecoveryPlanStatus.INVALIDATED, result.invalidatedPlan().status());
        assertEquals(SELECTED_AT, result.invalidatedPlan().selectedAt());
        assertEquals(GENERATED_AT, result.invalidatedPlan().generatedAt());
        assertSame(executing.evaluation(), result.invalidatedPlan().evaluation());
    }

    @Test
    void everyFrozenReasonMapsToItsApprovedExplanationCode() {
        Map<PlanInvalidationReason, String> expected = Map.ofEntries(
                Map.entry(PlanInvalidationReason.PROVIDER_CANCELLED,
                        RecoveryPlanInvalidator.PROVIDER_CANCELLED),
                Map.entry(PlanInvalidationReason.PROVIDER_AVAILABILITY_EXPIRED,
                        RecoveryPlanInvalidator.PROVIDER_AVAILABILITY_EXPIRED),
                Map.entry(PlanInvalidationReason.ETA_THRESHOLD_EXCEEDED,
                        RecoveryPlanInvalidator.PLAN_ETA_THRESHOLD_EXCEEDED),
                Map.entry(PlanInvalidationReason.VEHICLE_CONDITION_WORSENED,
                        RecoveryPlanInvalidator.VEHICLE_CONDITION_WORSENED),
                Map.entry(PlanInvalidationReason.ROUTE_BLOCKED,
                        RecoveryPlanInvalidator.ROUTE_BLOCKED),
                Map.entry(PlanInvalidationReason.CARGO_SAFETY_THREATENED,
                        RecoveryPlanInvalidator.CARGO_TEMPERATURE_AT_RISK),
                Map.entry(PlanInvalidationReason.DRIVER_TIME_INSUFFICIENT,
                        RecoveryPlanInvalidator.DRIVER_HOURS_INSUFFICIENT),
                Map.entry(PlanInvalidationReason.RESOURCE_UNAVAILABLE,
                        RecoveryPlanInvalidator.REQUIRED_RESOURCE_UNAVAILABLE),
                Map.entry(PlanInvalidationReason.HIGHER_SEVERITY_DISRUPTION,
                        RecoveryPlanInvalidator.HIGHER_SEVERITY_DISRUPTION_DETECTED)
        );

        expected.forEach((reason, code) -> assertEquals(code, invalidator.explanationCode(reason)));
    }

    @Test
    void replayingSameConfirmedEventProducesIdenticalOutcome() {
        RecoveryPlan activePlan = activePlan(RecoveryPlanStatus.SELECTED, PROVIDER_ID);
        ConfirmedPlanChange change = change(PlanInvalidationReason.RESOURCE_UNAVAILABLE);

        RecoveryPlanInvalidationResult first = invalidator.invalidate(activePlan, change);
        RecoveryPlanInvalidationResult replay = invalidator.invalidate(activePlan, change);

        assertEquals(first, replay);
        assertEquals(5, first.invalidation().id().version());
    }

    @Test
    void distinctSourceEventsProduceDistinctInvalidationIds() {
        RecoveryPlan activePlan = activePlan(RecoveryPlanStatus.SELECTED, PROVIDER_ID);
        RecoveryPlanInvalidationResult first = invalidator.invalidate(
                activePlan,
                change(PlanInvalidationReason.PROVIDER_CANCELLED)
        );
        ConfirmedPlanChange later = new ConfirmedPlanChange(
                id("10000000-0000-4000-9000-000000000004"),
                PlanInvalidationReason.PROVIDER_CANCELLED,
                OBSERVED_AT.plusSeconds(60),
                "Provider cancellation confirmed again"
        );

        RecoveryPlanInvalidationResult second = invalidator.invalidate(activePlan, later);

        assertNotSame(first.invalidation(), second.invalidation());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                first.invalidation().id(),
                second.invalidation().id()
        );
    }

    @Test
    void invalidationDoesNotMutateTheActivePlan() {
        RecoveryPlan original = activePlan(RecoveryPlanStatus.SELECTED, PROVIDER_ID);

        RecoveryPlanInvalidationResult result = invalidator.invalidate(
                original,
                change(PlanInvalidationReason.VEHICLE_CONDITION_WORSENED)
        );

        assertEquals(RecoveryPlanStatus.SELECTED, original.status());
        assertEquals(SELECTED_AT, original.selectedAt());
        assertNotSame(original, result.invalidatedPlan());
    }

    @Test
    void eventCannotPrecedePlanSelection() {
        ConfirmedPlanChange stale = new ConfirmedPlanChange(
                EVENT_ID,
                PlanInvalidationReason.ROUTE_BLOCKED,
                SELECTED_AT.minusSeconds(1),
                "Old route event"
        );

        assertThrows(IllegalArgumentException.class, () -> invalidator.invalidate(
                activePlan(RecoveryPlanStatus.SELECTED, PROVIDER_ID),
                stale
        ));
    }

    @Test
    void onlySelectedOrExecutingPlansCanBeInvalidated() {
        for (RecoveryPlanStatus status : List.of(
                RecoveryPlanStatus.GENERATED,
                RecoveryPlanStatus.EVALUATING,
                RecoveryPlanStatus.FEASIBLE,
                RecoveryPlanStatus.REJECTED,
                RecoveryPlanStatus.INVALIDATED,
                RecoveryPlanStatus.COMPLETED,
                RecoveryPlanStatus.FAILED
        )) {
            assertThrows(IllegalArgumentException.class, () -> invalidator.invalidate(
                    activePlan(status, PROVIDER_ID),
                    change(PlanInvalidationReason.ROUTE_BLOCKED)
            ));
        }
    }

    @Test
    void providerSpecificChangeRequiresProviderBoundPlan() {
        RecoveryPlan emergency = activePlan(RecoveryPlanStatus.SELECTED, null);

        for (PlanInvalidationReason reason : List.of(
                PlanInvalidationReason.PROVIDER_CANCELLED,
                PlanInvalidationReason.PROVIDER_AVAILABILITY_EXPIRED,
                PlanInvalidationReason.ETA_THRESHOLD_EXCEEDED,
                PlanInvalidationReason.RESOURCE_UNAVAILABLE
        )) {
            assertThrows(IllegalArgumentException.class, () -> invalidator.invalidate(
                    emergency,
                    change(reason)
            ));
        }
    }

    @Test
    void detailsRespectFrozenApiMaximumLength() {
        assertThrows(IllegalArgumentException.class, () -> new ConfirmedPlanChange(
                EVENT_ID,
                PlanInvalidationReason.ROUTE_BLOCKED,
                OBSERVED_AT,
                "x".repeat(501)
        ));

        ConfirmedPlanChange maximum = new ConfirmedPlanChange(
                EVENT_ID,
                PlanInvalidationReason.ROUTE_BLOCKED,
                OBSERVED_AT,
                "x".repeat(500)
        );
        RecoveryPlanInvalidationResult result = invalidator.invalidate(
                activePlan(RecoveryPlanStatus.SELECTED, PROVIDER_ID),
                maximum
        );
        assertEquals(500, result.invalidation().details().length());
    }

    @Test
    void malformedActivePlanEvaluationCannotBeInvalidated() {
        RecoveryPlan valid = activePlan(RecoveryPlanStatus.SELECTED, PROVIDER_ID);
        PlanEvaluation evaluation = valid.evaluation();
        PlanEvaluation rejectedEvaluation = new PlanEvaluation(
                evaluation.planId(), false, List.of("HARD-RULE-001"), evaluation.warnings(),
                evaluation.softConstraintResults(), evaluation.riskScore(), evaluation.uncertaintyScore(),
                evaluation.benefitScore(), evaluation.lambdaCoefficient(), evaluation.muCoefficient(),
                evaluation.finalUtility(), evaluation.confidence(), evaluation.freshnessSummary(),
                evaluation.explanationCodes(), evaluation.evaluatedAt(), evaluation.evaluatorVersion(),
                evaluation.inputHash()
        );
        RecoveryPlan malformed = copy(valid, rejectedEvaluation);

        assertThrows(IllegalArgumentException.class, () -> invalidator.invalidate(
                malformed,
                change(PlanInvalidationReason.ROUTE_BLOCKED)
        ));
    }

    private static ConfirmedPlanChange change(PlanInvalidationReason reason) {
        return new ConfirmedPlanChange(EVENT_ID, reason, OBSERVED_AT, "Confirmed plan condition change");
    }

    private static RecoveryPlan activePlan(RecoveryPlanStatus status, UUID providerId) {
        PlanEvaluation evaluation = evaluation();
        return new RecoveryPlan(
                PLAN_ID,
                TRIP_ID,
                DISRUPTION_ID,
                providerId == null
                        ? RecoveryPlanType.EMERGENCY_ESCALATION
                        : RecoveryPlanType.WORKSHOP_REPAIR,
                status,
                providerId,
                null,
                List.of(new RecoveryPlanStep(
                        1,
                        "RECOVERY_ACTION",
                        "Perform recovery action",
                        providerId,
                        SELECTED_AT,
                        SELECTED_AT.plusSeconds(1800),
                        List.of()
                )),
                evaluation,
                GENERATED_AT,
                SELECTED_AT
        );
    }

    private static PlanEvaluation evaluation() {
        return new PlanEvaluation(
                PLAN_ID,
                true,
                List.of(),
                List.of(),
                Map.of("TEST_SCORE", new BigDecimal("80.0000")),
                new BigDecimal("10.0000"),
                new BigDecimal("5.0000"),
                new BigDecimal("80.0000"),
                new BigDecimal("0.3500"),
                new BigDecimal("0.2000"),
                new BigDecimal("75.5000"),
                0.95,
                Map.of("vehicle.health", "FRESH"),
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"),
                SELECTED_AT,
                RecoveryPlanScorer.EVALUATOR_VERSION,
                "a".repeat(64)
        );
    }

    private static RecoveryPlan copy(RecoveryPlan plan, PlanEvaluation evaluation) {
        return new RecoveryPlan(
                plan.id(), plan.tripId(), plan.disruptionId(), plan.type(), plan.status(), plan.providerId(),
                plan.safeStopId(), plan.steps(), evaluation, plan.generatedAt(), plan.selectedAt()
        );
    }

    private static UUID id(String value) {
        return UUID.fromString(value);
    }
}
