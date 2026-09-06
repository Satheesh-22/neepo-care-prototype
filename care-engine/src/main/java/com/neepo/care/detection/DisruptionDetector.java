package com.neepo.care.detection;

import com.neepo.care.context.ContextFreshnessResult;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.context.DecisionState;
import com.neepo.care.context.ObservationFreshnessAssessment;
import com.neepo.contracts.CargoContext;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DisruptionDetector {
    public static final String OVERHEATING_RULE = "VEHICLE-OVERHEAT-DETECT-001";
    public static final String CARGO_TEMPERATURE_RULE = "CARGO-TEMP-DETECT-001";
    public static final String CARGO_TEMPERATURE_CODE = "CARGO_TEMPERATURE_AT_RISK";

    private static final String VEHICLE_OBSERVATION = "vehicle.health";
    private static final String CARGO_OBSERVATION = "cargo.condition";
    private static final EnumSet<TripStatus> ACTIVE_TRIP_STATUSES = EnumSet.of(
            TripStatus.IN_PROGRESS,
            TripStatus.DISRUPTED,
            TripStatus.RECOVERY_IN_PROGRESS
    );

    private final ContextFreshnessValidator freshnessValidator;
    private final DisruptionDetectionPolicy policy;

    public DisruptionDetector(
            ContextFreshnessValidator freshnessValidator,
            DisruptionDetectionPolicy policy
    ) {
        this.freshnessValidator = Objects.requireNonNull(freshnessValidator, "Freshness validator is required");
        this.policy = Objects.requireNonNull(policy, "Detection policy is required");
    }

    public DisruptionDetectionResult detect(TripContext context) {
        DecisionState state = DecisionState.from(context);
        ContextFreshnessResult freshness = freshnessValidator.validate(state);
        Map<String, ObservationFreshnessAssessment> observations = freshness.assessments().stream()
                .collect(Collectors.toUnmodifiableMap(
                        ObservationFreshnessAssessment::key,
                        Function.identity()
                ));

        List<RuleOutcome> outcomes;
        if (context.trip().status() == null || !ACTIVE_TRIP_STATUSES.contains(context.trip().status())) {
            outcomes = List.of(
                    insufficient(DisruptionType.VEHICLE_OVERHEATING, "TRIP_NOT_ACTIVE"),
                    insufficient(DisruptionType.CARGO_TEMPERATURE_DEVIATION, "TRIP_NOT_ACTIVE")
            );
        } else {
            outcomes = List.of(
                    evaluateVehicleOverheating(context, observations, state),
                    evaluateCargoTemperature(context, observations, state)
            );
        }

        List<DetectedDisruption> disruptions = outcomes.stream()
                .map(RuleOutcome::disruption)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .<DetectedDisruption>comparingInt(disruption -> severityRank(disruption.severity()))
                        .reversed()
                        .thenComparingInt(disruption -> disruption.type().ordinal()))
                .toList();
        List<DisruptionRuleAssessment> assessments = outcomes.stream()
                .map(RuleOutcome::assessment)
                .sorted(Comparator.comparingInt(assessment -> assessment.type().ordinal()))
                .toList();
        return new DisruptionDetectionResult(freshness, disruptions, assessments);
    }

    private RuleOutcome evaluateVehicleOverheating(
            TripContext context,
            Map<String, ObservationFreshnessAssessment> observations,
            DecisionState state
    ) {
        VehicleContext vehicle = context.vehicle();
        List<String> blockers = new ArrayList<>();
        if (!usable(observations, VEHICLE_OBSERVATION)) {
            blockers.add("VEHICLE_HEALTH_OBSERVATION_UNUSABLE");
        }
        if (vehicle.currentEngineTemperatureCelsius() == null) {
            blockers.add("ENGINE_TEMPERATURE_MISSING");
        }
        if (vehicle.maximumEngineTemperatureCelsius() == null) {
            blockers.add("ENGINE_TEMPERATURE_LIMIT_MISSING");
        }
        if (!blockers.isEmpty()) {
            return insufficient(DisruptionType.VEHICLE_OVERHEATING, blockers);
        }

        BigDecimal observed = vehicle.currentEngineTemperatureCelsius();
        BigDecimal maximum = vehicle.maximumEngineTemperatureCelsius();
        if (observed.compareTo(maximum) <= 0) {
            return clear(DisruptionType.VEHICLE_OVERHEATING);
        }

        DetectedDisruption disruption = new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                policy.vehicleOverheatingSeverity(),
                state.evaluationTime(),
                List.of(),
                Map.of(
                        "contextHash", state.contextHash(),
                        "excessCelsius", observed.subtract(maximum),
                        "maximumTemperatureCelsius", maximum,
                        "observedTemperatureCelsius", observed,
                        "ruleId", OVERHEATING_RULE
                )
        );
        return detected(disruption);
    }

    private RuleOutcome evaluateCargoTemperature(
            TripContext context,
            Map<String, ObservationFreshnessAssessment> observations,
            DecisionState state
    ) {
        CargoContext cargo = context.cargo();
        BigDecimal minimum = cargo.minimumTemperatureCelsius();
        BigDecimal maximum = cargo.maximumTemperatureCelsius();
        if (minimum == null && maximum == null) {
            return clear(DisruptionType.CARGO_TEMPERATURE_DEVIATION);
        }

        List<String> blockers = new ArrayList<>();
        if (!usable(observations, CARGO_OBSERVATION)) {
            blockers.add("CARGO_CONDITION_OBSERVATION_UNUSABLE");
        }
        if (cargo.currentTemperatureCelsius() == null) {
            blockers.add("CARGO_TEMPERATURE_MISSING");
        }
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            blockers.add("CARGO_TEMPERATURE_RANGE_INVALID");
        }
        if (!blockers.isEmpty()) {
            return insufficient(DisruptionType.CARGO_TEMPERATURE_DEVIATION, blockers);
        }

        BigDecimal observed = cargo.currentTemperatureCelsius();
        String direction;
        BigDecimal deviation;
        BigDecimal limit;
        if (minimum != null && observed.compareTo(minimum) < 0) {
            direction = "BELOW_MINIMUM";
            deviation = minimum.subtract(observed);
            limit = minimum;
        } else if (maximum != null && observed.compareTo(maximum) > 0) {
            direction = "ABOVE_MAXIMUM";
            deviation = observed.subtract(maximum);
            limit = maximum;
        } else {
            return clear(DisruptionType.CARGO_TEMPERATURE_DEVIATION);
        }

        DetectedDisruption disruption = new DetectedDisruption(
                DisruptionType.CARGO_TEMPERATURE_DEVIATION,
                policy.cargoTemperatureDeviationSeverity(),
                state.evaluationTime(),
                List.of(CARGO_TEMPERATURE_CODE),
                Map.of(
                        "contextHash", state.contextHash(),
                        "deviationCelsius", deviation,
                        "direction", direction,
                        "limitTemperatureCelsius", limit,
                        "observedTemperatureCelsius", observed,
                        "ruleId", CARGO_TEMPERATURE_RULE
                )
        );
        return detected(disruption);
    }

    private static boolean usable(
            Map<String, ObservationFreshnessAssessment> observations,
            String key
    ) {
        ObservationFreshnessAssessment assessment = observations.get(key);
        return assessment != null && assessment.usableForDecision();
    }

    private static RuleOutcome detected(DetectedDisruption disruption) {
        return new RuleOutcome(
                new DisruptionRuleAssessment(disruption.type(), DetectionRuleStatus.DETECTED, List.of()),
                disruption
        );
    }

    private static RuleOutcome clear(DisruptionType type) {
        return new RuleOutcome(
                new DisruptionRuleAssessment(type, DetectionRuleStatus.CLEAR, List.of()),
                null
        );
    }

    private static RuleOutcome insufficient(DisruptionType type, String reason) {
        return insufficient(type, List.of(reason));
    }

    private static RuleOutcome insufficient(DisruptionType type, List<String> reasons) {
        return new RuleOutcome(
                new DisruptionRuleAssessment(type, DetectionRuleStatus.INSUFFICIENT_CONTEXT, reasons),
                null
        );
    }

    private static int severityRank(DisruptionSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private record RuleOutcome(DisruptionRuleAssessment assessment, DetectedDisruption disruption) {
    }
}
