package com.neepo.backend.service;

import com.neepo.backend.dto.CreateDisruptionRequest;
import com.neepo.backend.dto.ProcessedEventRecord;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.contracts.DisruptionEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

@Service
@Transactional(readOnly = true)
public class DisruptionService {
    private static final String EVENT_TYPE = "DISRUPTION_REPORTED";

    private final TripRepository trips;
    private final DisruptionJdbcRepository disruptions;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public DisruptionService(
            TripRepository trips,
            DisruptionJdbcRepository disruptions,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this(trips, disruptions, objectMapper, clock, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DisruptionService(
            TripRepository trips,
            DisruptionJdbcRepository disruptions,
            ObjectMapper objectMapper,
            Clock clock,
            @Nullable ApplicationEventPublisher events
    ) {
        this.trips = trips;
        this.disruptions = disruptions;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public DisruptionEvent report(UUID companyId, CreateDisruptionRequest request) {
        Trip trip = trips.findByIdAndCompanyId(request.tripId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        String payloadHash = payloadHash(request);

        var processed = disruptions.findProcessedEvent(request.eventId());
        if (processed.isPresent()) {
            return resolveDuplicate(companyId, request.eventId(), payloadHash, processed.get());
        }
        if (!trip.acceptsDisruption()) {
            throw new ResourceConflictException(
                    "TRIP_NOT_ACTIVE",
                    "Disruptions can only be reported for an active trip"
            );
        }
        if (!disruptions.contextBelongsToTrip(companyId, request.tripId(), request.contextSnapshotId())) {
            throw new ResourceNotFoundException(
                    "CONTEXT_SNAPSHOT_NOT_FOUND",
                    "Context snapshot was not found for this trip"
            );
        }

        if (!disruptions.insertIfAbsent(request)) {
            ProcessedEventRecord concurrent = disruptions.findProcessedEvent(request.eventId())
                    .orElseThrow(DisruptionService::incompleteIdempotencyState);
            return resolveDuplicate(companyId, request.eventId(), payloadHash, concurrent);
        }

        DisruptionEvent result = disruptions.findByEventId(companyId, request.eventId())
                .orElseThrow(DisruptionService::incompleteIdempotencyState);
        disruptions.recordProcessedEvent(
                request.eventId(),
                request.tripId(),
                payloadHash,
                clock.instant(),
                result.id()
        );
        trip.markDisrupted();
        if (events != null) {
            events.publishEvent(new DisruptionReportedEvent(companyId, result));
        }
        return result;
    }

    private DisruptionEvent resolveDuplicate(
            UUID companyId,
            UUID eventId,
            String payloadHash,
            ProcessedEventRecord processed
    ) {
        if (!EVENT_TYPE.equals(processed.eventType()) || !payloadHash.equals(processed.payloadHash())) {
            throw new ResourceConflictException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "The eventId has already been used with a different payload"
            );
        }
        DisruptionEvent result = disruptions.findByEventId(companyId, eventId)
                .orElseThrow(DisruptionService::incompleteIdempotencyState);
        if (!result.id().equals(processed.resultReferenceId())) {
            throw incompleteIdempotencyState();
        }
        return result;
    }

    private String payloadHash(CreateDisruptionRequest request) {
        try {
            byte[] canonical = objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (JacksonException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not hash disruption payload", exception);
        }
    }

    private static ResourceConflictException incompleteIdempotencyState() {
        return new ResourceConflictException(
                "IDEMPOTENCY_STATE_INCOMPLETE",
                "The event was previously recorded without a matching result"
        );
    }
}
