package com.neepo.care.context;

import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.ProviderContext;
import com.neepo.contracts.RouteRestriction;
import com.neepo.contracts.SafeStopContext;
import com.neepo.contracts.TelemetryObservation;
import com.neepo.contracts.TripContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Immutable, freshness-relevant projection of a frozen shared {@link TripContext}.
 */
public record DecisionState(
        UUID tripId,
        Instant evaluationTime,
        String contextHash,
        List<DecisionObservation> observations
) {
    private static final Pattern SHA_256 = Pattern.compile("^[a-fA-F0-9]{64}$");

    public DecisionState {
        Objects.requireNonNull(tripId, "Trip id is required");
        Objects.requireNonNull(evaluationTime, "Evaluation time is required");
        if (contextHash == null || !SHA_256.matcher(contextHash).matches()) {
            throw new IllegalArgumentException("Context hash must be a 64-character SHA-256 hex value");
        }
        if (observations == null) {
            throw new IllegalArgumentException("Observations are required");
        }
        observations = observations.stream()
                .sorted(Comparator.comparing(DecisionObservation::key))
                .toList();
        HashSet<String> keys = new HashSet<>();
        for (DecisionObservation observation : observations) {
            if (!keys.add(observation.key())) {
                throw new IllegalArgumentException("Duplicate observation key: " + observation.key());
            }
        }
    }

    public static DecisionState from(TripContext context) {
        Objects.requireNonNull(context, "Trip context is required");
        Objects.requireNonNull(context.trip(), "Trip context trip is required");
        Objects.requireNonNull(context.vehicle(), "Vehicle context is required");
        Objects.requireNonNull(context.driver(), "Driver context is required");
        Objects.requireNonNull(context.cargo(), "Cargo context is required");
        Objects.requireNonNull(context.route(), "Route context is required");

        List<DecisionObservation> observations = new ArrayList<>();
        observations.add(hard("vehicle.health", context.vehicle().healthObservation()));
        observations.add(hard("driver.status", context.driver().statusObservation()));
        observations.add(hard("cargo.condition", context.cargo().conditionObservation()));
        observations.add(hard("route.condition", context.route().conditionObservation()));

        for (RouteRestriction restriction : values(context.route().restrictions())) {
            observations.add(hard(
                    "route.restriction." + restriction.restrictionId(),
                    restriction.observation()
            ));
        }
        for (ProviderContext provider : values(context.providers())) {
            observations.add(advisory(
                    "provider." + provider.providerId() + ".availability",
                    provider.availabilityObservation()
            ));
        }
        for (SafeStopContext safeStop : values(context.safeStops())) {
            observations.add(advisory(
                    "safeStop." + safeStop.safeStopId() + ".status",
                    safeStop.observation()
            ));
        }

        latestTelemetry(context.telemetryObservations()).ifPresent(telemetry -> observations.add(hard(
                "telemetry.latest",
                new ObservationMetadata(
                        telemetry.recordedAt(),
                        telemetry.receivedAt(),
                        telemetry.expiresAt(),
                        telemetry.confidence(),
                        telemetry.observationStatus(),
                        telemetry.source() == null ? null : telemetry.source().name()
                )
        )));

        return new DecisionState(
                context.trip().id(),
                context.evaluationTime(),
                context.contextHash(),
                observations
        );
    }

    private static DecisionObservation hard(String key, ObservationMetadata metadata) {
        return new DecisionObservation(key, ObservationRequirement.HARD_FACT, metadata);
    }

    private static DecisionObservation advisory(String key, ObservationMetadata metadata) {
        return new DecisionObservation(key, ObservationRequirement.ADVISORY, metadata);
    }

    private static java.util.Optional<TelemetryObservation> latestTelemetry(List<TelemetryObservation> telemetry) {
        Comparator<TelemetryObservation> order = Comparator
                .comparing(
                        TelemetryObservation::recordedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(
                        TelemetryObservation::eventId,
                        Comparator.nullsLast(Comparator.comparing(UUID::toString))
                );
        return values(telemetry).stream().max(order);
    }

    private static <T> List<T> values(List<T> values) {
        return values == null ? List.of() : values;
    }
}
