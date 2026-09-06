package com.neepo.backend.service;

import com.neepo.backend.dto.TripContextSnapshotRecord;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.TripContextSnapshotJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.care.context.ContextFreshnessPolicy;
import com.neepo.care.context.ContextFreshnessResult;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.context.DecisionState;
import com.neepo.contracts.TripContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TripContextSnapshotService {
    private final TripRepository trips;
    private final TripContextSnapshotJdbcRepository snapshots;
    private final ContextFreshnessValidator freshnessValidator;
    private final ObjectMapper objectMapper;

    public TripContextSnapshotService(
            TripRepository trips,
            TripContextSnapshotJdbcRepository snapshots,
            ObjectMapper objectMapper
    ) {
        this.trips = Objects.requireNonNull(trips, "Trip repository is required");
        this.snapshots = Objects.requireNonNull(snapshots, "Context snapshot repository is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper is required");
        this.freshnessValidator = new ContextFreshnessValidator(ContextFreshnessPolicy.defaults());
    }

    @Transactional
    public TripContextSnapshotRecord persist(
            UUID companyId,
            UUID tripId,
            TripContext context
    ) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(tripId, "Trip id is required");
        Objects.requireNonNull(context, "Trip context is required");
        Objects.requireNonNull(context.trip(), "Trip context trip is required");
        if (!tripId.equals(context.trip().id())) {
            throw new IllegalArgumentException("Trip context does not match the requested trip");
        }

        Trip trip = trips.findByIdAndCompanyId(tripId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        if (!trip.acceptsDisruption()) {
            throw new ResourceConflictException(
                    "TRIP_NOT_ACTIVE",
                    "Decision context can only be captured for an active trip"
            );
        }
        if (!snapshots.lockTrip(companyId, tripId)) {
            throw new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found");
        }

        DecisionState state = DecisionState.from(context);
        ContextFreshnessResult freshness = freshnessValidator.validate(state);
        TripContextSnapshotRecord snapshot = new TripContextSnapshotRecord(
                UUID.randomUUID(),
                tripId,
                snapshots.nextSequence(tripId),
                state.evaluationTime(),
                BigDecimal.valueOf(freshness.minimumConfidence()).setScale(3, RoundingMode.HALF_UP),
                freshness.hasExpiredRequiredData(),
                Objects.requireNonNull(context.networkState(), "Trip context network state is required"),
                payload(context),
                state.contextHash().toLowerCase(Locale.ROOT)
        );
        snapshots.insert(snapshot);
        return snapshot;
    }

    public TripContext load(UUID companyId, UUID tripId, UUID snapshotId) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(tripId, "Trip id is required");
        Objects.requireNonNull(snapshotId, "Context snapshot id is required");
        TripContextSnapshotRecord snapshot = snapshots.findById(companyId, tripId, snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CONTEXT_SNAPSHOT_NOT_FOUND",
                        "Context snapshot was not found for this tenant trip"
                ));
        TripContext context = context(snapshot.contextPayload());
        DecisionState state = DecisionState.from(context);
        if (!tripId.equals(state.tripId())
                || !snapshot.capturedAt().equals(state.evaluationTime())
                || !snapshot.contextHash().equalsIgnoreCase(state.contextHash())) {
            throw new IllegalStateException("Persisted context snapshot integrity check failed");
        }
        return context;
    }

    private String payload(TripContext context) {
        try {
            return objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(context);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Trip context cannot be serialized", exception);
        }
    }

    private TripContext context(String payload) {
        try {
            return objectMapper.readValue(payload, TripContext.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted trip context cannot be deserialized", exception);
        }
    }
}
