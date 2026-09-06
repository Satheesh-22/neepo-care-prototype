package com.neepo.care.telemetry;

import com.neepo.care.context.ContextFreshnessResult;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.context.DecisionState;
import com.neepo.care.context.ObservationRequirement;
import com.neepo.contracts.ActiveDisruption;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.NetworkState;
import com.neepo.contracts.ObservationStatus;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.TelemetryPolicyState;
import com.neepo.contracts.TripContext;

import java.util.List;
import java.util.Objects;

public final class AdaptiveTelemetryPolicyEvaluator {
    private final ContextFreshnessValidator freshnessValidator;
    private final AdaptiveTelemetryPolicy policy;

    public AdaptiveTelemetryPolicyEvaluator(
            ContextFreshnessValidator freshnessValidator,
            AdaptiveTelemetryPolicy policy
    ) {
        this.freshnessValidator = Objects.requireNonNull(
                freshnessValidator,
                "Context freshness validator is required"
        );
        this.policy = Objects.requireNonNull(policy, "Adaptive telemetry policy is required");
    }

    public TelemetryPolicyDecision evaluate(TripContext context, TelemetryPolicySignal signal) {
        DecisionState state = DecisionState.from(context);
        Objects.requireNonNull(signal, "Telemetry policy signal is required");
        Objects.requireNonNull(context.networkState(), "Telemetry policy network state is required");
        if (signal.observedAt().isAfter(state.evaluationTime())) {
            throw new IllegalArgumentException("Telemetry policy signal cannot follow context evaluation time");
        }
        validateDisruptions(context.activeDisruptions(), state.evaluationTime());
        ContextFreshnessResult freshness = freshnessValidator.validate(state);

        TelemetryPolicyState policyState = state(context, signal, freshness);
        return new TelemetryPolicyDecision(
                state.tripId(),
                signal.eventId(),
                policyState,
                interval(policyState),
                state.evaluationTime(),
                policyState.name()
        );
    }

    private TelemetryPolicyState state(
            TripContext context,
            TelemetryPolicySignal signal,
            ContextFreshnessResult freshness
    ) {
        if (context.networkState() == NetworkState.OFFLINE) {
            return TelemetryPolicyState.OFFLINE_BUFFERING;
        }
        if (signal.safelyParked()) {
            return TelemetryPolicyState.SAFELY_PARKED;
        }
        if (emergencyPlan(context.currentRecoveryPlan()) || hasEmergencyDisruption(context.activeDisruptions())) {
            return TelemetryPolicyState.BREAKDOWN_OR_EMERGENCY;
        }
        if (hasRouteDeviation(context.activeDisruptions())) {
            return TelemetryPolicyState.ROUTE_DEVIATION;
        }
        if (signal.nearDecisionPoint()
                || invalidatedPlan(context.currentRecoveryPlan())
                || !freshness.hardFactsUsable()
                || freshness.assessments().stream()
                .anyMatch(assessment -> assessment.requirement() == ObservationRequirement.HARD_FACT
                        && assessment.status() == ObservationStatus.AGING)) {
            return TelemetryPolicyState.NEAR_DECISION_POINT;
        }
        return TelemetryPolicyState.NORMAL_TRAVEL;
    }

    private Integer interval(TelemetryPolicyState state) {
        return switch (state) {
            case NORMAL_TRAVEL -> policy.normalIntervalSeconds();
            case NEAR_DECISION_POINT -> policy.nearDecisionIntervalSeconds();
            case ROUTE_DEVIATION -> policy.routeDeviationIntervalSeconds();
            case BREAKDOWN_OR_EMERGENCY -> policy.emergencyIntervalSeconds();
            case SAFELY_PARKED -> policy.safelyParkedIntervalSeconds();
            case OFFLINE_BUFFERING -> null;
        };
    }

    private static boolean hasEmergencyDisruption(List<ActiveDisruption> disruptions) {
        return values(disruptions).stream().anyMatch(disruption ->
                disruption.type() != DisruptionType.ROUTE_DEVIATION
                        && (disruption.severity() == DisruptionSeverity.HIGH
                        || disruption.severity() == DisruptionSeverity.CRITICAL));
    }

    private static boolean hasRouteDeviation(List<ActiveDisruption> disruptions) {
        return values(disruptions).stream().anyMatch(disruption ->
                disruption.type() == DisruptionType.ROUTE_DEVIATION);
    }

    private static boolean emergencyPlan(RecoveryPlan plan) {
        return plan != null
                && plan.type() == RecoveryPlanType.EMERGENCY_ESCALATION
                && (plan.status() == RecoveryPlanStatus.SELECTED
                || plan.status() == RecoveryPlanStatus.EXECUTING);
    }

    private static boolean invalidatedPlan(RecoveryPlan plan) {
        return plan != null && plan.status() == RecoveryPlanStatus.INVALIDATED;
    }

    private static void validateDisruptions(List<ActiveDisruption> disruptions, java.time.Instant evaluationTime) {
        for (ActiveDisruption disruption : values(disruptions)) {
            Objects.requireNonNull(disruption, "Active disruption entry is required");
            Objects.requireNonNull(disruption.disruptionId(), "Active disruption id is required");
            Objects.requireNonNull(disruption.eventId(), "Active disruption event id is required");
            Objects.requireNonNull(disruption.type(), "Active disruption type is required");
            Objects.requireNonNull(disruption.severity(), "Active disruption severity is required");
            Objects.requireNonNull(disruption.detectedAt(), "Active disruption detection time is required");
            if (disruption.detectedAt().isAfter(evaluationTime)) {
                throw new IllegalArgumentException("Active disruption cannot follow context evaluation time");
            }
        }
    }

    private static <T> List<T> values(List<T> values) {
        return values == null ? List.of() : values;
    }
}
