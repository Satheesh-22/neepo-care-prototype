package com.neepo.care.evaluation;

import com.neepo.contracts.RecoveryPlanType;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record HardConstraintEvaluationPolicy(
        Duration providerAvailabilityMaximumAge,
        Map<RecoveryPlanType, List<String>> requiredProviderCapabilities
) {
    public HardConstraintEvaluationPolicy {
        if (providerAvailabilityMaximumAge == null
                || providerAvailabilityMaximumAge.isZero()
                || providerAvailabilityMaximumAge.isNegative()) {
            throw new IllegalArgumentException("Provider availability maximum age must be positive");
        }
        EnumMap<RecoveryPlanType, List<String>> capabilities = new EnumMap<>(RecoveryPlanType.class);
        if (requiredProviderCapabilities != null) {
            requiredProviderCapabilities.forEach((type, values) -> {
                if (type == null || values == null) {
                    throw new IllegalArgumentException("Capability policy entries must not be null");
                }
                List<String> normalized = values.stream()
                        .map(value -> {
                            if (value == null || value.isBlank()) {
                                throw new IllegalArgumentException("Required capability must not be blank");
                            }
                            return value;
                        })
                        .distinct()
                        .sorted()
                        .toList();
                capabilities.put(type, normalized);
            });
        }
        requiredProviderCapabilities = Collections.unmodifiableMap(capabilities);
    }

    public static HardConstraintEvaluationPolicy demoDefaults() {
        return new HardConstraintEvaluationPolicy(
                Duration.ofMinutes(10),
                Map.of(
                        RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC,
                        List.of("MOBILE_ENGINE_COOLING_REPAIR"),
                        RecoveryPlanType.TOW_REPAIR_OR_TRANSFER,
                        List.of("HEAVY_VEHICLE_TOWING")
                )
        );
    }

    public List<String> requiredCapabilities(RecoveryPlanType type) {
        return requiredProviderCapabilities.getOrDefault(type, List.of());
    }

    String canonicalForm() {
        StringBuilder value = new StringBuilder(providerAvailabilityMaximumAge.toString());
        requiredProviderCapabilities.forEach((type, capabilities) -> value
                .append('|')
                .append(type.name())
                .append('=')
                .append(String.join(",", capabilities)));
        return value.toString();
    }
}
