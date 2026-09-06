package com.neepo.care.evaluation;

import com.neepo.care.context.ContextFreshnessResult;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.context.DecisionObservation;
import com.neepo.care.context.DecisionState;
import com.neepo.care.context.FreshnessIssue;
import com.neepo.care.context.ObservationFreshnessAssessment;
import com.neepo.care.context.ObservationRequirement;
import com.neepo.contracts.CargoContext;
import com.neepo.contracts.CargoType;
import com.neepo.contracts.DriverStatus;
import com.neepo.contracts.NetworkState;
import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.ProviderAvailabilityStatus;
import com.neepo.contracts.ProviderContext;
import com.neepo.contracts.ProviderType;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.RouteRestriction;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.SafeStopContext;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.VehicleContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class HardConstraintEvaluator {
    public static final String EVALUATOR_VERSION = "hard-constraints-1.0.0";

    public static final String VEHICLE_COMPATIBILITY_RULE = "VEHICLE-COMPAT-001";
    public static final String ROUTE_DIMENSION_RULE = "ROUTE-DIM-001";
    public static final String CARGO_SAFETY_RULE = "CARGO-SAFE-001";
    public static final String CARGO_TEMPERATURE_RULE = "CARGO-TEMP-001";
    public static final String PROVIDER_CAPABILITY_RULE = "PROVIDER-CAP-001";
    public static final String PROVIDER_FRESHNESS_RULE = "PROVIDER-FRESH-001";
    public static final String RESOURCE_RULE = "RESOURCE-001";
    public static final String DRIVER_HOURS_RULE = "DRIVER-HOURS-001";
    public static final String SAFE_STOP_RULE = "SAFE-STOP-001";
    public static final String WAITING_TIME_RULE = "WAIT-001";
    public static final String DELIVERY_DEADLINE_RULE = "DEADLINE-001";
    public static final String ROUTE_BLOCK_RULE = "ROUTE-BLOCK-001";
    public static final String CONTEXT_FRESHNESS_RULE = "CONTEXT-FRESH-001";

    public static final String VEHICLE_TYPE_UNSUPPORTED = "VEHICLE_TYPE_UNSUPPORTED";
    public static final String ROAD_HEIGHT_LIMIT_EXCEEDED = "ROAD_HEIGHT_LIMIT_EXCEEDED";
    public static final String ROAD_WIDTH_LIMIT_EXCEEDED = "ROAD_WIDTH_LIMIT_EXCEEDED";
    public static final String ROAD_WEIGHT_LIMIT_EXCEEDED = "ROAD_WEIGHT_LIMIT_EXCEEDED";
    public static final String CARGO_REQUIREMENT_UNSUPPORTED = "CARGO_REQUIREMENT_UNSUPPORTED";
    public static final String CARGO_TEMPERATURE_AT_RISK = "CARGO_TEMPERATURE_AT_RISK";
    public static final String PROVIDER_CAPABILITY_MISSING = "PROVIDER_CAPABILITY_MISSING";
    public static final String PROVIDER_AVAILABILITY_EXPIRED = "PROVIDER_AVAILABILITY_EXPIRED";
    public static final String PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE";
    public static final String REQUIRED_EQUIPMENT_UNAVAILABLE = "REQUIRED_EQUIPMENT_UNAVAILABLE";
    public static final String DRIVER_HOURS_INSUFFICIENT = "DRIVER_HOURS_INSUFFICIENT";
    public static final String STOP_LOCATION_UNSAFE = "STOP_LOCATION_UNSAFE";
    public static final String WAITING_TIME_EXCEEDED = "WAITING_TIME_EXCEEDED";
    public static final String DELIVERY_DEADLINE_EXCEEDED = "DELIVERY_DEADLINE_EXCEEDED";
    public static final String ROUTE_BLOCKED = "ROUTE_BLOCKED";
    public static final String CONTEXT_OBSERVATION_EXPIRED = "CONTEXT_OBSERVATION_EXPIRED";
    public static final String LOW_CONTEXT_CONFIDENCE = "LOW_CONTEXT_CONFIDENCE";

    private static final List<String> RULE_ORDER = List.of(
            VEHICLE_COMPATIBILITY_RULE,
            ROUTE_DIMENSION_RULE,
            CARGO_SAFETY_RULE,
            CARGO_TEMPERATURE_RULE,
            PROVIDER_CAPABILITY_RULE,
            PROVIDER_FRESHNESS_RULE,
            RESOURCE_RULE,
            DRIVER_HOURS_RULE,
            SAFE_STOP_RULE,
            WAITING_TIME_RULE,
            DELIVERY_DEADLINE_RULE,
            ROUTE_BLOCK_RULE,
            CONTEXT_FRESHNESS_RULE
    );
    private static final Comparator<RecoveryPlan> PLAN_ORDER = Comparator
            .comparingInt((RecoveryPlan plan) -> plan.type().ordinal())
            .thenComparing(plan -> nullableId(plan.providerId()))
            .thenComparing(plan -> nullableId(plan.safeStopId()))
            .thenComparing(plan -> plan.id().toString());

    private final ContextFreshnessValidator freshnessValidator;
    private final HardConstraintEvaluationPolicy policy;

    public HardConstraintEvaluator(
            ContextFreshnessValidator freshnessValidator,
            HardConstraintEvaluationPolicy policy
    ) {
        this.freshnessValidator = Objects.requireNonNull(freshnessValidator, "Freshness validator is required");
        this.policy = Objects.requireNonNull(policy, "Hard-constraint policy is required");
    }

    public HardConstraintEvaluationResult evaluate(TripContext context, List<RecoveryPlan> plans) {
        DecisionState state = DecisionState.from(context);
        ContextFreshnessResult baseFreshness = freshnessValidator.validate(state);
        Map<UUID, ProviderContext> providers = providersById(context.providers());
        Map<UUID, SafeStopContext> safeStops = safeStopsById(context.safeStops());
        List<RecoveryPlan> orderedPlans = validateAndOrderPlans(state, plans);

        List<HardConstraintPlanEvaluation> evaluations = orderedPlans.stream()
                .map(plan -> evaluatePlan(context, state, baseFreshness, providers, safeStops, plan))
                .toList();
        return new HardConstraintEvaluationResult(evaluations);
    }

    private HardConstraintPlanEvaluation evaluatePlan(
            TripContext context,
            DecisionState state,
            ContextFreshnessResult baseFreshness,
            Map<UUID, ProviderContext> providers,
            Map<UUID, SafeStopContext> safeStops,
            RecoveryPlan plan
    ) {
        ProviderContext provider = plan.providerId() == null ? null : providers.get(plan.providerId());
        SafeStopContext safeStop = plan.safeStopId() == null ? null : safeStops.get(plan.safeStopId());
        PlanFreshness freshness = planFreshness(state, baseFreshness, plan, provider, safeStop);

        List<HardConstraintAssessment> assessments;
        if (plan.type() == RecoveryPlanType.EMERGENCY_ESCALATION) {
            assessments = RULE_ORDER.stream()
                    .map(rule -> notApplicable(rule, "Emergency escalation remains available without inferred confirmations"))
                    .toList();
        } else {
            assessments = List.of(
                    vehicleCompatibility(context.vehicle(), plan, provider),
                    routeDimensions(context, plan, provider, safeStop),
                    cargoSafety(context, plan, provider),
                    cargoTemperature(context.cargo(), context.vehicle()),
                    providerCapability(plan, provider),
                    providerFreshness(context, provider, freshness.providerAssessment()),
                    resourceAvailability(provider),
                    driverHours(context, plan),
                    safeStop(plan, safeStop, freshness.safeStopAssessment()),
                    waitingTime(context, provider),
                    deliveryDeadline(context, plan, provider),
                    routeBlocked(context, plan, provider, safeStop),
                    contextFreshness(context.networkState(), freshness)
            );
        }

        List<String> warnings = freshness.lowConfidence()
                || context.networkState() == NetworkState.WEAK
                ? List.of(LOW_CONTEXT_CONFIDENCE)
                : List.of();
        return new HardConstraintPlanEvaluation(
                plan,
                assessments,
                warnings,
                freshness.minimumConfidence(),
                freshness.summary(),
                state.evaluationTime(),
                EVALUATOR_VERSION,
                evaluationInputHash(state.contextHash(), plan)
        );
    }

    private HardConstraintAssessment vehicleCompatibility(
            VehicleContext vehicle,
            RecoveryPlan plan,
            ProviderContext provider
    ) {
        ProviderType expectedType = expectedProviderType(plan.type());
        boolean vehicleCapabilitiesMissing = (plan.type() == RecoveryPlanType.WORKSHOP_REPAIR
                || plan.type() == RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER)
                && vehicle.requiredCapabilities() == null;
        List<String> requiredVehicleCapabilities = plan.type() == RecoveryPlanType.WORKSHOP_REPAIR
                || plan.type() == RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER
                ? safeStrings(vehicle.requiredCapabilities())
                : List.of();
        List<String> providerCapabilities = provider == null
                ? List.of()
                : safeStrings(provider.capabilities());
        List<String> missing = requiredVehicleCapabilities.stream()
                .filter(required -> !providerCapabilities.contains(required))
                .toList();
        boolean incompatible = provider == null
                || provider.providerType() != expectedType
                || vehicle.vehicleType() == null
                || vehicle.vehicleType().isBlank()
                || vehicleCapabilitiesMissing
                || !missing.isEmpty();

        Map<String, Object> evidence = evidence(
                "expectedProviderType", expectedType == null ? null : expectedType.name(),
                "actualProviderType", provider == null || provider.providerType() == null
                        ? null : provider.providerType().name(),
                "vehicleType", vehicle.vehicleType(),
                "vehicleCapabilitiesMissing", vehicleCapabilitiesMissing,
                "missingVehicleCapabilities", missing
        );
        return incompatible
                ? violated(VEHICLE_COMPATIBILITY_RULE, List.of(VEHICLE_TYPE_UNSUPPORTED), evidence)
                : satisfied(VEHICLE_COMPATIBILITY_RULE, evidence);
    }

    private HardConstraintAssessment routeDimensions(
            TripContext context,
            RecoveryPlan plan,
            ProviderContext provider,
            SafeStopContext safeStop
    ) {
        VehicleContext vehicle = context.vehicle();
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        List<String> violatedRestrictions = new ArrayList<>();
        for (RouteRestriction restriction : restrictions(context)) {
            if (!appliesToPlan(restriction, provider, safeStop)) {
                continue;
            }
            String kind = normalized(restriction.restrictionKind());
            BigDecimal actual = switch (kind) {
                case "HEIGHT" -> vehicle.heightMeters();
                case "WIDTH" -> vehicle.widthMeters();
                case "WEIGHT" -> vehicle.grossWeightKg();
                default -> null;
            };
            String code = dimensionCode(kind);
            if (code != null && exceeds(actual, restriction.limitValue())) {
                codes.add(code);
                violatedRestrictions.add(restriction.restrictionCode());
            }
        }

        if (safeStop != null) {
            if (exceeds(vehicle.heightMeters(), safeStop.maximumVehicleHeightMeters())) {
                codes.add(ROAD_HEIGHT_LIMIT_EXCEEDED);
                violatedRestrictions.add("safeStop.maximumVehicleHeightMeters");
            }
            if (exceeds(vehicle.grossWeightKg(), safeStop.maximumVehicleWeightKg())) {
                codes.add(ROAD_WEIGHT_LIMIT_EXCEEDED);
                violatedRestrictions.add("safeStop.maximumVehicleWeightKg");
            }
        }

        Map<String, Object> evidence = evidence(
                "vehicleHeightMeters", vehicle.heightMeters(),
                "vehicleWidthMeters", vehicle.widthMeters(),
                "vehicleGrossWeightKg", vehicle.grossWeightKg(),
                "violatedRestrictions", violatedRestrictions
        );
        return codes.isEmpty()
                ? satisfied(ROUTE_DIMENSION_RULE, evidence)
                : violated(ROUTE_DIMENSION_RULE, List.copyOf(codes), evidence);
    }

    private HardConstraintAssessment cargoSafety(
            TripContext context,
            RecoveryPlan plan,
            ProviderContext provider
    ) {
        CargoContext cargo = context.cargo();
        boolean temperatureControlled = cargo.cargoType() == CargoType.REFRIGERATED
                || cargo.cargoType() == CargoType.FROZEN;
        boolean transferRequirementsMissing = plan.type() == RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER
                && cargo.handlingRequirements() == null;
        List<String> missingTransferCapabilities = List.of();
        if (plan.type() == RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER) {
            List<String> providerCapabilities = provider == null
                    ? List.of()
                    : safeStrings(provider.capabilities());
            missingTransferCapabilities = safeStrings(cargo.handlingRequirements()).stream()
                    .filter(required -> !providerCapabilities.contains(required))
                    .toList();
        }
        boolean unsupported = cargo.cargoType() == null
                || temperatureControlled && !context.vehicle().refrigerationSupported()
                || cargo.hazardous()
                && plan.type() == RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER
                && (provider == null || !safeStrings(provider.capabilities()).contains("HAZARDOUS_CARGO_HANDLING"))
                || transferRequirementsMissing
                || !missingTransferCapabilities.isEmpty();
        Map<String, Object> evidence = evidence(
                "cargoType", cargo.cargoType() == null ? null : cargo.cargoType().name(),
                "refrigerationSupported", context.vehicle().refrigerationSupported(),
                "transferRequirementsMissing", transferRequirementsMissing,
                "missingTransferCapabilities", missingTransferCapabilities
        );
        return unsupported
                ? violated(CARGO_SAFETY_RULE, List.of(CARGO_REQUIREMENT_UNSUPPORTED), evidence)
                : satisfied(CARGO_SAFETY_RULE, evidence);
    }

    private HardConstraintAssessment cargoTemperature(CargoContext cargo, VehicleContext vehicle) {
        BigDecimal minimum = cargo.minimumTemperatureCelsius();
        BigDecimal maximum = cargo.maximumTemperatureCelsius();
        if (minimum == null && maximum == null) {
            return notApplicable(CARGO_TEMPERATURE_RULE, "Cargo has no configured temperature range");
        }
        BigDecimal current = cargo.currentTemperatureCelsius();
        boolean invalidRange = minimum != null && maximum != null && minimum.compareTo(maximum) > 0;
        boolean outside = current == null
                || minimum != null && current.compareTo(minimum) < 0
                || maximum != null && current.compareTo(maximum) > 0;
        boolean atRisk = invalidRange || outside || !vehicle.refrigerationSupported();
        Map<String, Object> evidence = evidence(
                "currentTemperatureCelsius", current,
                "minimumTemperatureCelsius", minimum,
                "maximumTemperatureCelsius", maximum,
                "refrigerationSupported", vehicle.refrigerationSupported()
        );
        return atRisk
                ? violated(CARGO_TEMPERATURE_RULE, List.of(CARGO_TEMPERATURE_AT_RISK), evidence)
                : satisfied(CARGO_TEMPERATURE_RULE, evidence);
    }

    private HardConstraintAssessment providerCapability(RecoveryPlan plan, ProviderContext provider) {
        List<String> required = policy.requiredCapabilities(plan.type());
        List<String> available = provider == null ? List.of() : safeStrings(provider.capabilities());
        List<String> missing = required.stream().filter(value -> !available.contains(value)).toList();
        boolean wrongType = provider == null || provider.providerType() != expectedProviderType(plan.type());
        Map<String, Object> evidence = evidence(
                "requiredCapabilities", required,
                "missingCapabilities", missing,
                "providerTypeMatches", !wrongType
        );
        return wrongType || !missing.isEmpty()
                ? violated(PROVIDER_CAPABILITY_RULE, List.of(PROVIDER_CAPABILITY_MISSING), evidence)
                : satisfied(PROVIDER_CAPABILITY_RULE, evidence);
    }

    private HardConstraintAssessment providerFreshness(
            TripContext context,
            ProviderContext provider,
            ObservationFreshnessAssessment providerAssessment
    ) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (provider == null || provider.availabilityStatus() != ProviderAvailabilityStatus.AVAILABLE) {
            codes.add(PROVIDER_UNAVAILABLE);
        }
        boolean tooOld = provider != null
                && provider.availabilityObservation() != null
                && provider.availabilityObservation().observedAt() != null
                && Duration.between(
                        provider.availabilityObservation().observedAt(),
                        context.evaluationTime()
                ).compareTo(policy.providerAvailabilityMaximumAge()) > 0;
        if (providerAssessment == null || !providerAssessment.usableForDecision() || tooOld) {
            codes.add(PROVIDER_AVAILABILITY_EXPIRED);
        }
        Map<String, Object> evidence = evidence(
                "availabilityStatus", provider == null || provider.availabilityStatus() == null
                        ? null : provider.availabilityStatus().name(),
                "observationStatus", providerAssessment == null || providerAssessment.status() == null
                        ? null : providerAssessment.status().name(),
                "maximumAge", policy.providerAvailabilityMaximumAge().toString(),
                "tooOld", tooOld
        );
        return codes.isEmpty()
                ? satisfied(PROVIDER_FRESHNESS_RULE, evidence)
                : violated(PROVIDER_FRESHNESS_RULE, List.copyOf(codes), evidence);
    }

    private HardConstraintAssessment resourceAvailability(ProviderContext provider) {
        boolean available = provider != null && provider.requiredResourceAvailable();
        Map<String, Object> evidence = evidence("requiredResourceAvailable", available);
        return available
                ? satisfied(RESOURCE_RULE, evidence)
                : violated(RESOURCE_RULE, List.of(REQUIRED_EQUIPMENT_UNAVAILABLE), evidence);
    }

    private HardConstraintAssessment driverHours(TripContext context, RecoveryPlan plan) {
        Duration requiredDriving = plan.steps().stream()
                .filter(step -> step.actionCode() != null && step.actionCode().startsWith("TRAVEL_"))
                .map(HardConstraintEvaluator::stepDuration)
                .reduce(Duration.ZERO, Duration::plus);
        if (requiredDriving.isZero()) {
            return notApplicable(DRIVER_HOURS_RULE, "The plan requires no driver-operated travel");
        }
        int remainingMinutes = context.driver().remainingDrivingMinutes();
        boolean statusBlocksDriving = context.driver().status() == DriverStatus.REST_REQUIRED
                || context.driver().status() == DriverStatus.RESTING
                || context.driver().status() == DriverStatus.OFF_DUTY;
        boolean insufficient = remainingMinutes < 0
                || requiredDriving.compareTo(Duration.ofMinutes(Math.max(remainingMinutes, 0))) > 0
                || statusBlocksDriving;
        Map<String, Object> evidence = evidence(
                "requiredDrivingDuration", requiredDriving.toString(),
                "remainingDrivingMinutes", remainingMinutes,
                "driverStatus", context.driver().status() == null ? null : context.driver().status().name()
        );
        return insufficient
                ? violated(DRIVER_HOURS_RULE, List.of(DRIVER_HOURS_INSUFFICIENT), evidence)
                : satisfied(DRIVER_HOURS_RULE, evidence);
    }

    private HardConstraintAssessment safeStop(
            RecoveryPlan plan,
            SafeStopContext safeStop,
            ObservationFreshnessAssessment safeStopAssessment
    ) {
        if (plan.type() != RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC) {
            return notApplicable(SAFE_STOP_RULE, "The plan does not bind a safe-stop facility");
        }
        boolean safe = safeStop != null
                && safeStop.securityVerified()
                && safeStopAssessment != null
                && safeStopAssessment.usableForDecision();
        Map<String, Object> evidence = evidence(
                "safeStopId", safeStop == null ? null : safeStop.safeStopId(),
                "securityVerified", safeStop != null && safeStop.securityVerified(),
                "observationUsable", safeStopAssessment != null && safeStopAssessment.usableForDecision()
        );
        return safe
                ? satisfied(SAFE_STOP_RULE, evidence)
                : violated(SAFE_STOP_RULE, List.of(STOP_LOCATION_UNSAFE), evidence);
    }

    private HardConstraintAssessment waitingTime(TripContext context, ProviderContext provider) {
        Integer estimatedArrival = provider == null ? null : provider.estimatedArrivalMinutes();
        int maximumWaiting = context.trip().maximumWaitingMinutes();
        boolean exceeded = estimatedArrival == null
                || estimatedArrival < 0
                || maximumWaiting < 0
                || estimatedArrival > maximumWaiting;
        Map<String, Object> evidence = evidence(
                "estimatedArrivalMinutes", estimatedArrival,
                "maximumWaitingMinutes", maximumWaiting
        );
        return exceeded
                ? violated(WAITING_TIME_RULE, List.of(WAITING_TIME_EXCEEDED), evidence)
                : satisfied(WAITING_TIME_RULE, evidence);
    }

    private HardConstraintAssessment deliveryDeadline(
            TripContext context,
            RecoveryPlan plan,
            ProviderContext provider
    ) {
        Instant deadline = context.trip().deliveryDeadline();
        Instant planComplete = plan.steps().stream()
                .map(RecoveryPlanStep::expectedCompleteAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        Integer providerWait = provider == null ? null : provider.estimatedArrivalMinutes();
        int routeMinutes = context.route().remainingDurationMinutes();
        Instant revisedArrival = planComplete == null || providerWait == null || providerWait < 0 || routeMinutes < 0
                ? null
                : planComplete.plus(Duration.ofMinutes((long) providerWait + routeMinutes));
        boolean exceeded = deadline == null || revisedArrival == null || revisedArrival.isAfter(deadline);
        Map<String, Object> evidence = evidence(
                "deliveryDeadline", deadline,
                "revisedArrival", revisedArrival,
                "providerWaitMinutes", providerWait,
                "remainingRouteMinutes", routeMinutes
        );
        return exceeded
                ? violated(DELIVERY_DEADLINE_RULE, List.of(DELIVERY_DEADLINE_EXCEEDED), evidence)
                : satisfied(DELIVERY_DEADLINE_RULE, evidence);
    }

    private HardConstraintAssessment routeBlocked(
            TripContext context,
            RecoveryPlan plan,
            ProviderContext provider,
            SafeStopContext safeStop
    ) {
        List<String> blockedRestrictions = restrictions(context).stream()
                .filter(restriction -> appliesToPlan(restriction, provider, safeStop))
                .filter(RouteRestriction::blocked)
                .map(RouteRestriction::restrictionCode)
                .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                .toList();
        boolean blocked = context.route().status() == null
                || context.route().status() == RouteStatus.BLOCKED
                || !blockedRestrictions.isEmpty();
        Map<String, Object> evidence = evidence(
                "routeStatus", context.route().status() == null ? null : context.route().status().name(),
                "blockedRestrictions", blockedRestrictions
        );
        return blocked
                ? violated(ROUTE_BLOCK_RULE, List.of(ROUTE_BLOCKED), evidence)
                : satisfied(ROUTE_BLOCK_RULE, evidence);
    }

    private HardConstraintAssessment contextFreshness(NetworkState networkState, PlanFreshness freshness) {
        boolean unavailableNetwork = networkState == null
                || networkState == NetworkState.OFFLINE
                || networkState == NetworkState.SYNCHRONIZING;
        boolean unusable = freshness.criticalAssessments().stream()
                .anyMatch(assessment -> !assessment.usableForDecision());
        Map<String, Object> evidence = evidence(
                "networkState", networkState == null ? null : networkState.name(),
                "unusableObservations", freshness.criticalAssessments().stream()
                        .filter(assessment -> !assessment.usableForDecision())
                        .map(ObservationFreshnessAssessment::key)
                        .toList()
        );
        return unavailableNetwork || unusable
                ? violated(CONTEXT_FRESHNESS_RULE, List.of(CONTEXT_OBSERVATION_EXPIRED), evidence)
                : satisfied(CONTEXT_FRESHNESS_RULE, evidence);
    }

    private PlanFreshness planFreshness(
            DecisionState state,
            ContextFreshnessResult baseFreshness,
            RecoveryPlan plan,
            ProviderContext provider,
            SafeStopContext safeStop
    ) {
        List<ObservationFreshnessAssessment> critical = new ArrayList<>(baseFreshness.assessments().stream()
                .filter(assessment -> assessment.requirement() == ObservationRequirement.HARD_FACT)
                .toList());
        TreeMap<String, String> summary = new TreeMap<>(baseFreshness.freshnessSummary());

        ObservationFreshnessAssessment providerAssessment = null;
        if (plan.type() != RecoveryPlanType.EMERGENCY_ESCALATION) {
            String key = "provider." + (plan.providerId() == null ? "missing" : plan.providerId()) + ".availability";
            providerAssessment = assessPromoted(
                    state,
                    key,
                    provider == null ? null : provider.availabilityObservation()
            );
            critical.add(providerAssessment);
            summary.put(key, providerAssessment.status().name());
        }

        ObservationFreshnessAssessment safeStopAssessment = null;
        if (plan.type() == RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC) {
            String key = "safeStop." + (plan.safeStopId() == null ? "missing" : plan.safeStopId()) + ".status";
            safeStopAssessment = assessPromoted(
                    state,
                    key,
                    safeStop == null ? null : safeStop.observation()
            );
            critical.add(safeStopAssessment);
            summary.put(key, safeStopAssessment.status().name());
        }

        double minimumConfidence = critical.stream()
                .mapToDouble(ObservationFreshnessAssessment::confidence)
                .min()
                .orElse(0);
        boolean lowConfidence = critical.stream().anyMatch(assessment ->
                assessment.issues().contains(FreshnessIssue.BELOW_MINIMUM_CONFIDENCE));
        return new PlanFreshness(
                List.copyOf(critical),
                providerAssessment,
                safeStopAssessment,
                minimumConfidence,
                lowConfidence,
                Collections.unmodifiableMap(new LinkedHashMap<>(summary))
        );
    }

    private ObservationFreshnessAssessment assessPromoted(
            DecisionState state,
            String key,
            ObservationMetadata metadata
    ) {
        DecisionState resourceState = new DecisionState(
                state.tripId(),
                state.evaluationTime(),
                state.contextHash(),
                List.of(new DecisionObservation(key, ObservationRequirement.HARD_FACT, metadata))
        );
        return freshnessValidator.validate(resourceState).assessments().getFirst();
    }

    private static List<RecoveryPlan> validateAndOrderPlans(DecisionState state, List<RecoveryPlan> plans) {
        Objects.requireNonNull(plans, "Recovery plans are required");
        Set<UUID> planIds = new HashSet<>();
        for (RecoveryPlan plan : plans) {
            Objects.requireNonNull(plan, "Recovery plan entry is required");
            Objects.requireNonNull(plan.id(), "Recovery plan id is required");
            Objects.requireNonNull(plan.type(), "Recovery plan type is required");
            if (!state.tripId().equals(plan.tripId())) {
                throw new IllegalArgumentException("Recovery plan trip does not match the context trip");
            }
            if (!state.evaluationTime().equals(plan.generatedAt())) {
                throw new IllegalArgumentException("Recovery plan generation time does not match the context evaluation time");
            }
            if (plan.status() != RecoveryPlanStatus.GENERATED || plan.evaluation() != null || plan.selectedAt() != null) {
                throw new IllegalArgumentException("Only unevaluated generated recovery plans may enter hard evaluation");
            }
            Objects.requireNonNull(plan.disruptionId(), "Recovery plan disruption id is required");
            Objects.requireNonNull(plan.steps(), "Recovery plan steps are required");
            validateSteps(plan);
            if (!planIds.add(plan.id())) {
                throw new IllegalArgumentException("Duplicate recovery plan id: " + plan.id());
            }
        }
        return plans.stream().sorted(PLAN_ORDER).toList();
    }

    private static void validateSteps(RecoveryPlan plan) {
        if (plan.steps().isEmpty()) {
            throw new IllegalArgumentException("Recovery plan must contain at least one step");
        }
        Instant expectedStart = plan.generatedAt();
        for (int index = 0; index < plan.steps().size(); index++) {
            RecoveryPlanStep step = Objects.requireNonNull(
                    plan.steps().get(index),
                    "Recovery plan step is required"
            );
            if (step.sequence() != index + 1
                    || step.actionCode() == null
                    || step.actionCode().isBlank()
                    || step.instruction() == null
                    || step.instruction().isBlank()
                    || step.expectedStartAt() == null
                    || step.expectedCompleteAt() == null
                    || !step.expectedStartAt().equals(expectedStart)
                    || !step.expectedCompleteAt().isAfter(step.expectedStartAt())
                    || step.requiredResources() == null
                    || step.requiredResources().stream().anyMatch(resource ->
                    resource == null || resource.isBlank())) {
                throw new IllegalArgumentException("Recovery plan contains an invalid step timeline");
            }
            expectedStart = step.expectedCompleteAt();
        }
    }

    private static Map<UUID, ProviderContext> providersById(List<ProviderContext> providers) {
        Map<UUID, ProviderContext> result = new HashMap<>();
        for (ProviderContext provider : providers == null ? List.<ProviderContext>of() : providers) {
            Objects.requireNonNull(provider, "Provider context entry is required");
            Objects.requireNonNull(provider.providerId(), "Provider id is required");
            if (result.put(provider.providerId(), provider) != null) {
                throw new IllegalArgumentException("Duplicate provider id: " + provider.providerId());
            }
        }
        return Map.copyOf(result);
    }

    private static Map<UUID, SafeStopContext> safeStopsById(List<SafeStopContext> safeStops) {
        Map<UUID, SafeStopContext> result = new HashMap<>();
        for (SafeStopContext safeStop : safeStops == null ? List.<SafeStopContext>of() : safeStops) {
            Objects.requireNonNull(safeStop, "Safe-stop context entry is required");
            Objects.requireNonNull(safeStop.safeStopId(), "Safe-stop id is required");
            if (result.put(safeStop.safeStopId(), safeStop) != null) {
                throw new IllegalArgumentException("Duplicate safe-stop id: " + safeStop.safeStopId());
            }
        }
        return Map.copyOf(result);
    }

    private static List<RouteRestriction> restrictions(TripContext context) {
        return context.route().restrictions() == null
                ? List.of()
                : context.route().restrictions().stream()
                .sorted(Comparator.comparing(
                        RouteRestriction::restrictionCode,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    private static boolean appliesToPlan(
            RouteRestriction restriction,
            ProviderContext provider,
            SafeStopContext safeStop
    ) {
        String code = normalized(restriction.restrictionCode());
        int marker = code.indexOf("_ACCESS_");
        if (marker < 0) {
            return true;
        }
        String target = code.substring(0, marker);
        return provider != null && normalized(provider.name()).startsWith(target)
                || safeStop != null && normalized(safeStop.name()).startsWith(target);
    }

    private static boolean exceeds(BigDecimal actual, BigDecimal limit) {
        return actual == null || limit == null || actual.compareTo(limit) > 0;
    }

    private static String dimensionCode(String kind) {
        return switch (kind) {
            case "HEIGHT" -> ROAD_HEIGHT_LIMIT_EXCEEDED;
            case "WIDTH" -> ROAD_WIDTH_LIMIT_EXCEEDED;
            case "WEIGHT" -> ROAD_WEIGHT_LIMIT_EXCEEDED;
            default -> null;
        };
    }

    private static ProviderType expectedProviderType(RecoveryPlanType type) {
        return switch (type) {
            case WORKSHOP_REPAIR -> ProviderType.WORKSHOP;
            case SAFE_PARKING_MOBILE_MECHANIC -> ProviderType.MOBILE_MECHANIC;
            case TOW_REPAIR_OR_TRANSFER -> ProviderType.TOWING_SERVICE;
            case SUBSTITUTE_VEHICLE_TRANSFER -> ProviderType.SUBSTITUTE_VEHICLE;
            case DRIVER_REST, EMERGENCY_ESCALATION -> null;
        };
    }

    private static Duration stepDuration(RecoveryPlanStep step) {
        if (step.expectedStartAt() == null
                || step.expectedCompleteAt() == null
                || !step.expectedCompleteAt().isAfter(step.expectedStartAt())) {
            return Duration.ofDays(36500);
        }
        return Duration.between(step.expectedStartAt(), step.expectedCompleteAt());
    }

    private String evaluationInputHash(String contextHash, RecoveryPlan plan) {
        StringBuilder canonical = new StringBuilder();
        appendHashField(canonical, contextHash);
        appendHashField(canonical, EVALUATOR_VERSION);
        appendHashField(canonical, policy.canonicalForm());
        appendHashField(canonical, plan.id());
        appendHashField(canonical, plan.tripId());
        appendHashField(canonical, plan.disruptionId());
        appendHashField(canonical, plan.type());
        appendHashField(canonical, plan.status());
        appendHashField(canonical, plan.providerId());
        appendHashField(canonical, plan.safeStopId());
        appendHashField(canonical, plan.generatedAt());
        for (RecoveryPlanStep step : plan.steps()) {
            appendHashField(canonical, step.sequence());
            appendHashField(canonical, step.actionCode());
            appendHashField(canonical, step.instruction());
            appendHashField(canonical, step.providerId());
            appendHashField(canonical, step.expectedStartAt());
            appendHashField(canonical, step.expectedCompleteAt());
            for (String resource : step.requiredResources()) {
                appendHashField(canonical, resource);
            }
        }
        return sha256(canonical.toString());
    }

    private static void appendHashField(StringBuilder canonical, Object value) {
        String text = value == null ? "<null>" : value.toString();
        canonical.append(text.length()).append(':').append(text).append('|');
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte current : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", current));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for evaluation hashing", exception);
        }
    }

    private static HardConstraintAssessment satisfied(String ruleId, Map<String, Object> evidence) {
        return new HardConstraintAssessment(ruleId, HardConstraintStatus.SATISFIED, List.of(), evidence);
    }

    private static HardConstraintAssessment violated(
            String ruleId,
            List<String> explanationCodes,
            Map<String, Object> evidence
    ) {
        return new HardConstraintAssessment(ruleId, HardConstraintStatus.VIOLATED, explanationCodes, evidence);
    }

    private static HardConstraintAssessment notApplicable(String ruleId, String reason) {
        return new HardConstraintAssessment(
                ruleId,
                HardConstraintStatus.NOT_APPLICABLE,
                List.of(),
                evidence("reason", reason)
        );
    }

    private static Map<String, Object> evidence(Object... entries) {
        TreeMap<String, Object> result = new TreeMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            Object value = entries[index + 1];
            if (value != null) {
                result.put((String) entries[index], value);
            }
        }
        return result;
    }

    private static List<String> safeStrings(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static String nullableId(UUID value) {
        return value == null ? "-" : value.toString();
    }

    private static String normalized(String value) {
        return value == null
                ? ""
                : value.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private record PlanFreshness(
            List<ObservationFreshnessAssessment> criticalAssessments,
            ObservationFreshnessAssessment providerAssessment,
            ObservationFreshnessAssessment safeStopAssessment,
            double minimumConfidence,
            boolean lowConfidence,
            Map<String, String> summary
    ) {
    }
}
