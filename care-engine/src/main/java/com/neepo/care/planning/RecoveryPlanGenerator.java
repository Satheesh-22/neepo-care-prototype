package com.neepo.care.planning;

import com.neepo.care.context.DecisionState;
import com.neepo.care.detection.DetectedDisruption;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.ProviderContext;
import com.neepo.contracts.ProviderType;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.SafeStopContext;
import com.neepo.contracts.TripContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure candidate expansion for a confirmed overheating disruption.
 *
 * <p>This stage intentionally does not evaluate freshness, capability, availability,
 * geometry, legal, cargo or timing constraints. Keeping every context-backed option
 * allows the hard-constraint evaluator to explain each rejection.</p>
 */
public final class RecoveryPlanGenerator {
    public static final String WORKSHOP_PROVIDER_MISSING = "WORKSHOP_PROVIDER_MISSING";
    public static final String MOBILE_MECHANIC_PROVIDER_MISSING = "MOBILE_MECHANIC_PROVIDER_MISSING";
    public static final String SAFE_STOP_MISSING = "SAFE_STOP_MISSING";
    public static final String TOWING_PROVIDER_MISSING = "TOWING_PROVIDER_MISSING";
    public static final String SUBSTITUTE_VEHICLE_PROVIDER_MISSING = "SUBSTITUTE_VEHICLE_PROVIDER_MISSING";
    public static final String DISRUPTION_NOT_APPLICABLE = "DISRUPTION_NOT_APPLICABLE";

    private static final UUID PLAN_NAMESPACE = UUID.fromString("11b88fe4-6c42-5eab-9166-849ccbeb93a6");
    private static final Comparator<ProviderContext> PROVIDER_ORDER = Comparator.comparing(
            provider -> provider.providerId().toString()
    );
    private static final Comparator<SafeStopContext> SAFE_STOP_ORDER = Comparator.comparing(
            safeStop -> safeStop.safeStopId().toString()
    );
    private static final Comparator<RecoveryPlan> PLAN_ORDER = Comparator
            .comparingInt((RecoveryPlan plan) -> plan.type().ordinal())
            .thenComparing(plan -> nullableId(plan.providerId()))
            .thenComparing(plan -> nullableId(plan.safeStopId()))
            .thenComparing(plan -> plan.id().toString());

    private final RecoveryPlanGenerationPolicy policy;

    public RecoveryPlanGenerator(RecoveryPlanGenerationPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "Generation policy is required");
    }

    public RecoveryPlanGenerationResult generate(
            TripContext context,
            UUID disruptionId,
            DetectedDisruption disruption
    ) {
        return generateCandidates(context, disruptionId, disruption, false);
    }

    public RecoveryPlanGenerationResult regenerate(
            TripContext context,
            UUID disruptionId,
            DetectedDisruption activeDisruption
    ) {
        return generateCandidates(context, disruptionId, activeDisruption, true);
    }

    private RecoveryPlanGenerationResult generateCandidates(
            TripContext context,
            UUID disruptionId,
            DetectedDisruption disruption,
            boolean replanning
    ) {
        DecisionState state = DecisionState.from(context);
        Objects.requireNonNull(disruptionId, "Disruption id is required");
        validateDisruption(state, disruption, replanning);

        List<ProviderContext> providers = providers(context.providers());
        List<SafeStopContext> safeStops = safeStops(context.safeStops());
        List<RecoveryPlan> plans = new ArrayList<>();
        EnumMap<RecoveryPlanType, String> skipped = new EnumMap<>(RecoveryPlanType.class);

        List<ProviderContext> workshops = providersOfType(providers, ProviderType.WORKSHOP);
        if (workshops.isEmpty()) {
            skipped.put(RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_PROVIDER_MISSING);
        } else {
            workshops.forEach(provider -> plans.add(plan(
                    state, disruptionId, RecoveryPlanType.WORKSHOP_REPAIR,
                    provider.providerId(), null, workshopSteps(state.evaluationTime(), provider.providerId())
            )));
        }

        List<ProviderContext> mobileMechanics = providersOfType(providers, ProviderType.MOBILE_MECHANIC);
        if (mobileMechanics.isEmpty()) {
            skipped.put(RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC, MOBILE_MECHANIC_PROVIDER_MISSING);
        } else if (safeStops.isEmpty()) {
            skipped.put(RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC, SAFE_STOP_MISSING);
        } else {
            for (ProviderContext provider : mobileMechanics) {
                for (SafeStopContext safeStop : safeStops) {
                    plans.add(plan(
                            state, disruptionId, RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC,
                            provider.providerId(), safeStop.safeStopId(),
                            mobileMechanicSteps(state.evaluationTime(), provider.providerId())
                    ));
                }
            }
        }

        List<ProviderContext> towingProviders = providersOfType(providers, ProviderType.TOWING_SERVICE);
        if (towingProviders.isEmpty()) {
            skipped.put(RecoveryPlanType.TOW_REPAIR_OR_TRANSFER, TOWING_PROVIDER_MISSING);
        } else {
            towingProviders.forEach(provider -> plans.add(plan(
                    state, disruptionId, RecoveryPlanType.TOW_REPAIR_OR_TRANSFER,
                    provider.providerId(), null, towingSteps(state.evaluationTime(), provider.providerId())
            )));
        }

        List<ProviderContext> substituteVehicles = providersOfType(providers, ProviderType.SUBSTITUTE_VEHICLE);
        if (substituteVehicles.isEmpty()) {
            skipped.put(RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER, SUBSTITUTE_VEHICLE_PROVIDER_MISSING);
        } else {
            substituteVehicles.forEach(provider -> plans.add(plan(
                    state, disruptionId, RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER,
                    provider.providerId(), null, transferSteps(state.evaluationTime(), provider.providerId())
            )));
        }

        skipped.put(RecoveryPlanType.DRIVER_REST, DISRUPTION_NOT_APPLICABLE);
        plans.add(plan(
                state, disruptionId, RecoveryPlanType.EMERGENCY_ESCALATION,
                null, null, emergencySteps(state.evaluationTime())
        ));

        plans.sort(PLAN_ORDER);
        return new RecoveryPlanGenerationResult(plans, skipped);
    }

    private static void validateDisruption(
            DecisionState state,
            DetectedDisruption disruption,
            boolean replanning
    ) {
        Objects.requireNonNull(disruption, "Detected disruption is required");
        if (disruption.type() != DisruptionType.VEHICLE_OVERHEATING) {
            throw new IllegalArgumentException("Only confirmed vehicle overheating is supported");
        }
        Objects.requireNonNull(disruption.detectedAt(), "Disruption detection time is required");
        if (disruption.detectedAt().isAfter(state.evaluationTime())) {
            throw new IllegalArgumentException("Disruption detection time cannot follow context evaluation time");
        }
        if (!replanning && !state.evaluationTime().equals(disruption.detectedAt())) {
            throw new IllegalArgumentException("Initial disruption detection time must match context evaluation time");
        }
        if (!replanning && !state.contextHash().equals(disruption.evidence().get("contextHash"))) {
            throw new IllegalArgumentException("Disruption evidence does not match the trip context hash");
        }
    }

    private RecoveryPlan plan(
            DecisionState state,
            UUID disruptionId,
            RecoveryPlanType type,
            UUID providerId,
            UUID safeStopId,
            List<RecoveryPlanStep> steps
    ) {
        UUID planId = planId(state, disruptionId, type, providerId, safeStopId);
        return new RecoveryPlan(
                planId,
                state.tripId(),
                disruptionId,
                type,
                RecoveryPlanStatus.GENERATED,
                providerId,
                safeStopId,
                List.copyOf(steps),
                null,
                state.evaluationTime(),
                null
        );
    }

    private List<RecoveryPlanStep> workshopSteps(Instant start, UUID providerId) {
        return timeline(start, List.of(
                new StepTemplate(
                        "STOP_AND_STABILIZE", "Stop safely and stabilize the overheating vehicle.",
                        null, policy.stabilizationDuration(), List.of("SAFE_STOPPING_LOCATION")
                ),
                new StepTemplate(
                        "TRAVEL_TO_WORKSHOP", "Proceed to the candidate workshop only after safety approval.",
                        providerId, policy.travelDuration(), List.of("SAFE_ROUTE", "WORKSHOP_CONFIRMATION")
                ),
                new StepTemplate(
                        "REPAIR_ENGINE_COOLING_SYSTEM", "Inspect and repair the engine cooling system.",
                        providerId, policy.workshopRepairDuration(), List.of("ENGINE_COOLING_REPAIR")
                )
        ));
    }

    private List<RecoveryPlanStep> mobileMechanicSteps(Instant start, UUID providerId) {
        return timeline(start, List.of(
                new StepTemplate(
                        "TRAVEL_TO_SAFE_STOP", "Proceed to the candidate safe stop only after safety approval.",
                        null, policy.travelDuration(), List.of("SAFE_ROUTE", "SAFE_PARKING_LOCATION")
                ),
                new StepTemplate(
                        "PARK_AND_STABILIZE", "Park safely and stabilize the overheating vehicle.",
                        null, policy.parkingDuration(), List.of("SAFE_PARKING_LOCATION")
                ),
                new StepTemplate(
                        "MOBILE_MECHANIC_REPAIR", "Inspect and repair the engine cooling system on site.",
                        providerId, policy.mobileRepairDuration(), List.of("MOBILE_ENGINE_COOLING_REPAIR")
                )
        ));
    }

    private List<RecoveryPlanStep> towingSteps(Instant start, UUID providerId) {
        return timeline(start, List.of(
                new StepTemplate(
                        "SECURE_VEHICLE_FOR_TOWING", "Stop safely and prepare the vehicle for towing.",
                        providerId, policy.towPreparationDuration(), List.of("SAFE_STOPPING_LOCATION", "HEAVY_VEHICLE_TOWING")
                ),
                new StepTemplate(
                        "TOW_TO_REPAIR_OR_TRANSFER", "Tow the vehicle to an approved repair or transfer location.",
                        providerId, policy.towingDuration(), List.of("HEAVY_VEHICLE_TOWING", "APPROVED_DESTINATION")
                )
        ));
    }

    private List<RecoveryPlanStep> transferSteps(Instant start, UUID providerId) {
        return timeline(start, List.of(
                new StepTemplate(
                        "PREPARE_CARGO_TRANSFER", "Secure the stopped vehicle and prepare the cargo for transfer.",
                        null, policy.transferPreparationDuration(), List.of("SAFE_TRANSFER_LOCATION", "CARGO_HANDLING_TEAM")
                ),
                new StepTemplate(
                        "TRANSFER_CARGO_TO_SUBSTITUTE", "Transfer the cargo to the candidate substitute vehicle.",
                        providerId, policy.cargoTransferDuration(), List.of("SUBSTITUTE_VEHICLE", "CARGO_HANDLING_TEAM")
                )
        ));
    }

    private List<RecoveryPlanStep> emergencySteps(Instant start) {
        return timeline(start, List.of(new StepTemplate(
                "STOP_SAFELY_AND_ESCALATE",
                "Stop immediately at the safest verifiable location and contact emergency support.",
                null,
                policy.emergencyEscalationDuration(),
                List.of("EMERGENCY_SUPPORT", "SAFE_STOPPING_LOCATION")
        )));
    }

    private static List<RecoveryPlanStep> timeline(Instant start, List<StepTemplate> templates) {
        List<RecoveryPlanStep> steps = new ArrayList<>(templates.size());
        Instant cursor = start;
        for (int index = 0; index < templates.size(); index++) {
            StepTemplate template = templates.get(index);
            Instant completion = cursor.plus(template.duration());
            steps.add(new RecoveryPlanStep(
                    index + 1,
                    template.actionCode(),
                    template.instruction(),
                    template.providerId(),
                    cursor,
                    completion,
                    template.requiredResources()
            ));
            cursor = completion;
        }
        return List.copyOf(steps);
    }

    private static List<ProviderContext> providers(List<ProviderContext> providers) {
        if (providers == null) {
            return List.of();
        }
        providers.forEach(provider -> {
            Objects.requireNonNull(provider, "Provider context entry is required");
            Objects.requireNonNull(provider.providerId(), "Provider id is required");
            Objects.requireNonNull(provider.providerType(), "Provider type is required");
        });
        return providers.stream().sorted(PROVIDER_ORDER).toList();
    }

    private static List<SafeStopContext> safeStops(List<SafeStopContext> safeStops) {
        if (safeStops == null) {
            return List.of();
        }
        safeStops.forEach(safeStop -> {
            Objects.requireNonNull(safeStop, "Safe-stop context entry is required");
            Objects.requireNonNull(safeStop.safeStopId(), "Safe-stop id is required");
        });
        return safeStops.stream().sorted(SAFE_STOP_ORDER).toList();
    }

    private static List<ProviderContext> providersOfType(
            List<ProviderContext> providers,
            ProviderType providerType
    ) {
        return providers.stream()
                .filter(provider -> provider.providerType() == providerType)
                .toList();
    }

    private static UUID planId(
            DecisionState state,
            UUID disruptionId,
            RecoveryPlanType type,
            UUID providerId,
            UUID safeStopId
    ) {
        String name = String.join("|",
                state.tripId().toString(),
                disruptionId.toString(),
                state.contextHash(),
                type.name(),
                nullableId(providerId),
                nullableId(safeStopId)
        );
        return uuidVersion5(PLAN_NAMESPACE, name);
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
            ByteBuffer uuidBytes = ByteBuffer.wrap(hash);
            return new UUID(uuidBytes.getLong(), uuidBytes.getLong());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is required for deterministic UUID generation", exception);
        }
    }

    private static String nullableId(UUID value) {
        return value == null ? "-" : value.toString();
    }

    private record StepTemplate(
            String actionCode,
            String instruction,
            UUID providerId,
            Duration duration,
            List<String> requiredResources
    ) {
    }
}
