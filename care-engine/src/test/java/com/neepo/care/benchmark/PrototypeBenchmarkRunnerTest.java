package com.neepo.care.benchmark;

import com.neepo.care.evaluation.HardConstraintEvaluator;
import com.neepo.care.scoring.RecoveryPlanScorer;
import com.neepo.care.selection.RecoveryPlanSelectionResult;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.TelemetryPolicyState;
import com.neepo.care.telemetry.TelemetryPolicyDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrototypeBenchmarkRunnerTest {
    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
    private static final UUID TRIP_ID = id("00000000-0000-4000-8600-000000000001");
    private static final UUID DISRUPTION_ID = id("00000000-0000-4000-9000-000000000001");
    private static final UUID NEAREST_PLAN = id("00000000-0000-4000-9100-000000000001");
    private static final UUID CARE_PLAN = id("00000000-0000-4000-9100-000000000002");
    private static final UUID NEAREST_PROVIDER = id("00000000-0000-4000-8700-000000000001");
    private static final UUID CARE_PROVIDER = id("00000000-0000-4000-8700-000000000002");

    private final TelemetryBenchmarkRunner telemetryRunner = new TelemetryBenchmarkRunner();
    private final PrototypeBenchmarkRunner runner = new PrototypeBenchmarkRunner();

    @Test
    void comparesCareWithUnsafeNearestProviderAndFixedTelemetry() {
        TelemetryBenchmarkComparison telemetry = telemetryRunner.compare(windows(), 60, 512);

        PrototypeBenchmarkReport report = runner.run(
                metadata(),
                careSelection(),
                Map.of(NEAREST_PROVIDER, 5, CARE_PROVIDER, 20),
                telemetry,
                12,
                2
        );

        StrategyMetrics care = report.strategies().getFirst();
        StrategyMetrics baseline = report.strategies().get(1);
        assertEquals(PrototypeBenchmarkRunner.CARE_STRATEGY, care.strategyCode());
        assertTrue(care.successfulRecovery());
        assertEquals(0, care.invalidRecommendations());
        assertEquals(900, care.recoveryTimeSeconds());
        assertEquals(13, care.telemetryTransmissions());
        assertEquals(13L * 512, care.estimatedBandwidthBytes());
        assertEquals(12, care.decisionLatencyMs());
        assertEquals(0, care.staleDataDecisions());

        assertEquals(PrototypeBenchmarkRunner.BASELINE_STRATEGY, baseline.strategyCode());
        assertFalse(baseline.successfulRecovery());
        assertEquals(1, baseline.invalidRecommendations());
        assertEquals(600, baseline.recoveryTimeSeconds());
        assertEquals(5, baseline.telemetryTransmissions());
        assertEquals(5L * 512, baseline.estimatedBandwidthBytes());
        assertEquals(2, baseline.decisionLatencyMs());
        assertEquals(1, baseline.staleDataDecisions());
    }

    @Test
    void reportPreservesRequiredExperimentProvenanceAndConfiguration() {
        PrototypeBenchmarkReport report = runner.run(
                metadata(), careSelection(), Map.of(NEAREST_PROVIDER, 5, CARE_PROVIDER, 20),
                telemetryRunner.compare(windows(), 60, 512), 12, 2
        );

        assertEquals("OVERHEAT-PROVIDER-CANCEL-001", report.metadata().scenarioCode());
        assertEquals("demo-v1", report.metadata().datasetVersion());
        assertEquals("Java 21 / deterministic unit fixture", report.metadata().environment());
        assertEquals("6d6f282", report.metadata().commitHash());
        assertEquals(List.of("No wall-clock latency is inferred"), report.metadata().limitations());
        assertEquals("60", report.configuration().get("fixedTelemetryIntervalSeconds"));
        assertEquals("512", report.configuration().get("telemetryPayloadBytes"));
        assertEquals("MEASURED_INPUT", report.configuration().get("latencySource"));
        assertThrows(UnsupportedOperationException.class, () ->
                report.configuration().put("mutation", "forbidden"));
    }

    @Test
    void nearestProviderTieUsesCanonicalPlanUuid() {
        RecoveryPlan first = rejectedPlan(NEAREST_PLAN, NEAREST_PROVIDER);
        UUID tiedProvider = id("00000000-0000-4000-8700-000000000003");
        RecoveryPlan second = rejectedPlan(
                id("00000000-0000-4000-9100-000000000003"), tiedProvider
        );
        RecoveryPlanSelectionResult selection = selection(List.of(second, selectedCarePlan(), first));

        PrototypeBenchmarkReport report = runner.run(
                metadata(), selection, Map.of(NEAREST_PROVIDER, 5, tiedProvider, 5, CARE_PROVIDER, 20),
                telemetryRunner.compare(List.of(), 60, 512), 1, 1
        );

        assertEquals(600, report.strategies().get(1).recoveryTimeSeconds());
        assertEquals(1, report.strategies().get(1).invalidRecommendations());
    }

    @Test
    void planAndEtaMapInputOrderCannotChangeBenchmark() {
        List<RecoveryPlan> plans = new ArrayList<>(List.of(rejectedNearestPlan(), selectedCarePlan()));
        PrototypeBenchmarkReport forward = runner.run(
                metadata(), selection(plans), Map.of(NEAREST_PROVIDER, 5, CARE_PROVIDER, 20),
                telemetryRunner.compare(windows(), 60, 512), 12, 2
        );
        Collections.reverse(plans);

        PrototypeBenchmarkReport reversed = runner.run(
                metadata(), selection(plans), Map.of(CARE_PROVIDER, 20, NEAREST_PROVIDER, 5),
                telemetryRunner.compare(windows(), 60, 512), 12, 2
        );

        assertEquals(forward, reversed);
    }

    @Test
    void adaptiveAndFixedTransmissionCountsUseSameOnlineWindows() {
        TelemetryBenchmarkComparison result = telemetryRunner.compare(windows(), 60, 512);

        assertEquals(13, result.adaptiveTransmissions());
        assertEquals(5, result.fixedTransmissions());
        assertEquals(13L * 512, result.adaptiveBandwidthBytes());
        assertEquals(5L * 512, result.fixedBandwidthBytes());
    }

    @Test
    void partialWindowRoundsUpToOneTransmission() {
        TelemetryPolicyWindow partial = window(
                1, NOW, NOW.plusSeconds(1), TelemetryPolicyState.NORMAL_TRAVEL, 60
        );

        TelemetryBenchmarkComparison result = telemetryRunner.compare(List.of(partial), 60, 100);

        assertEquals(1, result.adaptiveTransmissions());
        assertEquals(1, result.fixedTransmissions());
    }

    @Test
    void offlineWindowProducesNoTransmissionsForEitherStrategy() {
        TelemetryPolicyWindow offline = window(
                1, NOW, NOW.plusSeconds(600), TelemetryPolicyState.OFFLINE_BUFFERING, null
        );

        TelemetryBenchmarkComparison result = telemetryRunner.compare(List.of(offline), 60, 100);

        assertEquals(0, result.adaptiveTransmissions());
        assertEquals(0, result.fixedTransmissions());
        assertEquals(0, result.adaptiveBandwidthBytes());
        assertEquals(0, result.fixedBandwidthBytes());
    }

    @Test
    void telemetryWindowOrderDoesNotChangeResults() {
        List<TelemetryPolicyWindow> windows = new ArrayList<>(windows());
        TelemetryBenchmarkComparison forward = telemetryRunner.compare(windows, 60, 512);
        Collections.reverse(windows);

        TelemetryBenchmarkComparison reversed = telemetryRunner.compare(windows, 60, 512);

        assertEquals(forward, reversed);
    }

    @Test
    void overlappingTelemetryWindowsAreRejected() {
        List<TelemetryPolicyWindow> overlap = List.of(
                window(1, NOW, NOW.plusSeconds(60), TelemetryPolicyState.NORMAL_TRAVEL, 60),
                window(2, NOW.plusSeconds(30), NOW.plusSeconds(90),
                        TelemetryPolicyState.BREAKDOWN_OR_EMERGENCY, 3)
        );

        assertThrows(IllegalArgumentException.class, () ->
                telemetryRunner.compare(overlap, 60, 512));
    }

    @Test
    void nearestProviderBaselineRequiresNormalProviderCandidate() {
        RecoveryPlan emergency = plan(
                id("00000000-0000-4000-9100-000000000009"),
                null,
                RecoveryPlanType.EMERGENCY_ESCALATION,
                RecoveryPlanStatus.SELECTED,
                true,
                List.of(),
                300
        );
        RecoveryPlanSelectionResult selection = selection(List.of(emergency), emergency);

        assertThrows(IllegalArgumentException.class, () -> runner.run(
                metadata(), selection, Map.of(), telemetryRunner.compare(List.of(), 60, 512), 1, 1
        ));
    }

    @Test
    void invalidConfigurationAndMeasuredLatencyAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                telemetryRunner.compare(windows(), 0, 512));
        assertThrows(IllegalArgumentException.class, () -> runner.run(
                metadata(), careSelection(), Map.of(NEAREST_PROVIDER, -1, CARE_PROVIDER, 20),
                telemetryRunner.compare(windows(), 60, 512), 1, 1
        ));
        assertThrows(IllegalArgumentException.class, () -> runner.run(
                metadata(), careSelection(), Map.of(NEAREST_PROVIDER, 5, CARE_PROVIDER, 20),
                telemetryRunner.compare(windows(), 60, 512), -1, 1
        ));
    }

    @Test
    void benchmarkRecordsRejectNegativeRawMeasures() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyMetrics(
                "INVALID", false, -1, 0, 0, 0, 0, 0, 0
        ));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkMetadata(
                "", "demo-v1", "test", "commit", NOW, List.of()
        ));
    }

    private static List<TelemetryPolicyWindow> windows() {
        return List.of(
                window(1, NOW, NOW.plusSeconds(120), TelemetryPolicyState.NORMAL_TRAVEL, 60),
                window(2, NOW.plusSeconds(120), NOW.plusSeconds(150),
                        TelemetryPolicyState.BREAKDOWN_OR_EMERGENCY, 3),
                window(3, NOW.plusSeconds(150), NOW.plusSeconds(270),
                        TelemetryPolicyState.SAFELY_PARKED, 120),
                window(4, NOW.plusSeconds(270), NOW.plusSeconds(330),
                        TelemetryPolicyState.OFFLINE_BUFFERING, null)
        );
    }

    private static TelemetryPolicyWindow window(
            int suffix,
            Instant startsAt,
            Instant endsAt,
            TelemetryPolicyState state,
            Integer interval
    ) {
        return new TelemetryPolicyWindow(
                new TelemetryPolicyDecision(
                        TRIP_ID,
                        id("10000000-0000-4000-9300-00000000000" + suffix),
                        state,
                        interval,
                        startsAt,
                        state.name()
                ),
                endsAt
        );
    }

    private static BenchmarkMetadata metadata() {
        return new BenchmarkMetadata(
                "OVERHEAT-PROVIDER-CANCEL-001",
                "demo-v1",
                "Java 21 / deterministic unit fixture",
                "6d6f282",
                NOW,
                List.of("No wall-clock latency is inferred")
        );
    }

    private static RecoveryPlanSelectionResult careSelection() {
        return selection(List.of(rejectedNearestPlan(), selectedCarePlan()));
    }

    private static RecoveryPlanSelectionResult selection(List<RecoveryPlan> plans) {
        return selection(plans, selectedCarePlan());
    }

    private static RecoveryPlanSelectionResult selection(
            List<RecoveryPlan> plans,
            RecoveryPlan selected
    ) {
        return new RecoveryPlanSelectionResult(
                plans,
                selected,
                new DecisionExplanation(
                        TRIP_ID,
                        DISRUPTION_ID,
                        selected.id(),
                        plans.stream().filter(plan -> !plan.evaluation().feasible())
                                .map(RecoveryPlan::id).sorted().toList(),
                        List.of("PLAN_SELECTED_HIGHEST_UTILITY"),
                        Map.of(),
                        "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC",
                        NOW
                )
        );
    }

    private static RecoveryPlan rejectedNearestPlan() {
        return rejectedPlan(NEAREST_PLAN, NEAREST_PROVIDER);
    }

    private static RecoveryPlan rejectedPlan(UUID planId, UUID providerId) {
        return plan(
                planId,
                providerId,
                RecoveryPlanType.WORKSHOP_REPAIR,
                RecoveryPlanStatus.REJECTED,
                false,
                List.of(HardConstraintEvaluator.PROVIDER_AVAILABILITY_EXPIRED),
                600
        );
    }

    private static RecoveryPlan selectedCarePlan() {
        return plan(
                CARE_PLAN,
                CARE_PROVIDER,
                RecoveryPlanType.WORKSHOP_REPAIR,
                RecoveryPlanStatus.SELECTED,
                true,
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"),
                900
        );
    }

    private static RecoveryPlan plan(
            UUID planId,
            UUID providerId,
            RecoveryPlanType type,
            RecoveryPlanStatus status,
            boolean feasible,
            List<String> explanationCodes,
            int durationSeconds
    ) {
        PlanEvaluation evaluation = new PlanEvaluation(
                planId,
                feasible,
                feasible ? List.of() : List.of("PROVIDER-FRESH-001"),
                List.of(),
                Map.of(),
                BigDecimal.ZERO.setScale(4),
                BigDecimal.ZERO.setScale(4),
                BigDecimal.ZERO.setScale(4),
                new BigDecimal("0.3500"),
                new BigDecimal("0.2000"),
                BigDecimal.ZERO.setScale(4),
                0.95,
                Map.of(),
                explanationCodes,
                NOW,
                RecoveryPlanScorer.EVALUATOR_VERSION,
                "a".repeat(64)
        );
        return new RecoveryPlan(
                planId,
                TRIP_ID,
                DISRUPTION_ID,
                type,
                status,
                providerId,
                null,
                List.of(new RecoveryPlanStep(
                        1,
                        "RECOVERY_ACTION",
                        "Perform recovery action",
                        providerId,
                        NOW,
                        NOW.plusSeconds(durationSeconds),
                        List.of()
                )),
                evaluation,
                NOW,
                status == RecoveryPlanStatus.SELECTED ? NOW : null
        );
    }

    private static UUID id(String value) {
        return UUID.fromString(value);
    }
}
