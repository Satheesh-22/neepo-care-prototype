package com.neepo.care.invalidation;

import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.PlanInvalidation;
import com.neepo.contracts.PlanInvalidationReason;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RecoveryPlanInvalidator {
    public static final String PROVIDER_CANCELLED = "PROVIDER_CANCELLED";
    public static final String PROVIDER_AVAILABILITY_EXPIRED = "PROVIDER_AVAILABILITY_EXPIRED";
    public static final String PLAN_ETA_THRESHOLD_EXCEEDED = "PLAN_ETA_THRESHOLD_EXCEEDED";
    public static final String VEHICLE_CONDITION_WORSENED = "VEHICLE_CONDITION_WORSENED";
    public static final String REQUIRED_RESOURCE_UNAVAILABLE = "REQUIRED_RESOURCE_UNAVAILABLE";
    public static final String HIGHER_SEVERITY_DISRUPTION_DETECTED = "HIGHER_SEVERITY_DISRUPTION_DETECTED";
    public static final String ROUTE_BLOCKED = "ROUTE_BLOCKED";
    public static final String CARGO_TEMPERATURE_AT_RISK = "CARGO_TEMPERATURE_AT_RISK";
    public static final String DRIVER_HOURS_INSUFFICIENT = "DRIVER_HOURS_INSUFFICIENT";

    private static final UUID INVALIDATION_NAMESPACE = UUID.fromString(
            "e315553a-30b2-5c62-b1a7-0cdbd09c5bc1"
    );
    private static final Map<PlanInvalidationReason, String> EXPLANATION_CODES = explanationCodes();

    public RecoveryPlanInvalidationResult invalidate(
            RecoveryPlan activePlan,
            ConfirmedPlanChange change
    ) {
        validateActivePlan(activePlan);
        Objects.requireNonNull(change, "Confirmed plan change is required");
        validateChange(activePlan, change);

        UUID invalidationId = uuidVersion5(
                INVALIDATION_NAMESPACE,
                String.join("|", activePlan.id().toString(), change.eventId().toString(), change.reason().name())
        );
        PlanInvalidation invalidation = new PlanInvalidation(
                invalidationId,
                change.eventId(),
                activePlan.id(),
                change.reason(),
                EXPLANATION_CODES.get(change.reason()),
                change.observedAt(),
                change.details()
        );
        RecoveryPlan invalidatedPlan = new RecoveryPlan(
                activePlan.id(),
                activePlan.tripId(),
                activePlan.disruptionId(),
                activePlan.type(),
                RecoveryPlanStatus.INVALIDATED,
                activePlan.providerId(),
                activePlan.safeStopId(),
                List.copyOf(activePlan.steps()),
                activePlan.evaluation(),
                activePlan.generatedAt(),
                activePlan.selectedAt()
        );
        ReplanningTrigger trigger = new ReplanningTrigger(
                activePlan.tripId(),
                activePlan.disruptionId(),
                invalidationId,
                change.eventId(),
                change.observedAt()
        );
        return new RecoveryPlanInvalidationResult(invalidatedPlan, invalidation, trigger);
    }

    public String explanationCode(PlanInvalidationReason reason) {
        Objects.requireNonNull(reason, "Invalidation reason is required");
        return EXPLANATION_CODES.get(reason);
    }

    private static void validateActivePlan(RecoveryPlan plan) {
        Objects.requireNonNull(plan, "Active recovery plan is required");
        Objects.requireNonNull(plan.id(), "Active recovery plan id is required");
        Objects.requireNonNull(plan.tripId(), "Active recovery plan trip id is required");
        Objects.requireNonNull(plan.disruptionId(), "Active recovery plan disruption id is required");
        Objects.requireNonNull(plan.type(), "Active recovery plan type is required");
        Objects.requireNonNull(plan.steps(), "Active recovery plan steps are required");
        Objects.requireNonNull(plan.generatedAt(), "Active recovery plan generation time is required");
        Objects.requireNonNull(plan.selectedAt(), "Active recovery plan selection time is required");
        if (plan.status() != RecoveryPlanStatus.SELECTED
                && plan.status() != RecoveryPlanStatus.EXECUTING) {
            throw new IllegalArgumentException("Only SELECTED or EXECUTING plans can be invalidated");
        }
        PlanEvaluation evaluation = Objects.requireNonNull(
                plan.evaluation(),
                "Active recovery plan evaluation is required"
        );
        if (!plan.id().equals(evaluation.planId()) || !evaluation.feasible()) {
            throw new IllegalArgumentException("Active recovery plan must have a matching feasible evaluation");
        }
        if (plan.selectedAt().isBefore(plan.generatedAt())) {
            throw new IllegalArgumentException("Plan selection time cannot precede generation time");
        }
    }

    private static void validateChange(RecoveryPlan plan, ConfirmedPlanChange change) {
        if (change.observedAt().isBefore(plan.selectedAt())) {
            throw new IllegalArgumentException("Invalidation event cannot precede plan selection");
        }
        if (requiresProvider(change.reason()) && plan.providerId() == null) {
            throw new IllegalArgumentException("Provider-specific invalidation requires a provider-bound plan");
        }
    }

    private static boolean requiresProvider(PlanInvalidationReason reason) {
        return switch (reason) {
            case PROVIDER_CANCELLED,
                 PROVIDER_AVAILABILITY_EXPIRED,
                 ETA_THRESHOLD_EXCEEDED,
                 RESOURCE_UNAVAILABLE -> true;
            case VEHICLE_CONDITION_WORSENED,
                 ROUTE_BLOCKED,
                 CARGO_SAFETY_THREATENED,
                 DRIVER_TIME_INSUFFICIENT,
                 HIGHER_SEVERITY_DISRUPTION -> false;
        };
    }

    private static Map<PlanInvalidationReason, String> explanationCodes() {
        EnumMap<PlanInvalidationReason, String> codes = new EnumMap<>(PlanInvalidationReason.class);
        codes.put(PlanInvalidationReason.PROVIDER_CANCELLED, PROVIDER_CANCELLED);
        codes.put(PlanInvalidationReason.PROVIDER_AVAILABILITY_EXPIRED, PROVIDER_AVAILABILITY_EXPIRED);
        codes.put(PlanInvalidationReason.ETA_THRESHOLD_EXCEEDED, PLAN_ETA_THRESHOLD_EXCEEDED);
        codes.put(PlanInvalidationReason.VEHICLE_CONDITION_WORSENED, VEHICLE_CONDITION_WORSENED);
        codes.put(PlanInvalidationReason.ROUTE_BLOCKED, ROUTE_BLOCKED);
        codes.put(PlanInvalidationReason.CARGO_SAFETY_THREATENED, CARGO_TEMPERATURE_AT_RISK);
        codes.put(PlanInvalidationReason.DRIVER_TIME_INSUFFICIENT, DRIVER_HOURS_INSUFFICIENT);
        codes.put(PlanInvalidationReason.RESOURCE_UNAVAILABLE, REQUIRED_RESOURCE_UNAVAILABLE);
        codes.put(
                PlanInvalidationReason.HIGHER_SEVERITY_DISRUPTION,
                HIGHER_SEVERITY_DISRUPTION_DETECTED
        );
        if (codes.size() != PlanInvalidationReason.values().length) {
            throw new IllegalStateException("Every invalidation reason must have one explanation code");
        }
        return Map.copyOf(codes);
    }

    private static UUID uuidVersion5(UUID namespace, String name) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            ByteBuffer namespaceBytes = ByteBuffer.allocate(16)
                    .putLong(namespace.getMostSignificantBits())
                    .putLong(namespace.getLeastSignificantBits());
            sha1.update(namespaceBytes.array());
            byte[] hash = sha1.digest(name.getBytes(StandardCharsets.UTF_8));
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
            ByteBuffer result = ByteBuffer.wrap(hash, 0, 16);
            return new UUID(result.getLong(), result.getLong());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is required for deterministic UUID generation", exception);
        }
    }
}
