package com.neepo.backend.service;

import com.neepo.backend.domain.TripStatus;
import com.neepo.backend.dto.CreateDisruptionRequest;
import com.neepo.backend.dto.ProcessedEventRecord;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisruptionServiceTest {
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID CONTEXT_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID DISRUPTION_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");

    private final TripRepository trips = mock(TripRepository.class);
    private final DisruptionJdbcRepository repository = mock(DisruptionJdbcRepository.class);
    private final DisruptionService service = new DisruptionService(
            trips,
            repository,
            JsonMapper.builder().build(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void recordsDisruptionIdempotencyStateAndTransitionsTrip() {
        Trip trip = activeTrip();
        CreateDisruptionRequest request = request(DisruptionSeverity.HIGH);
        DisruptionEvent result = event(DisruptionSeverity.HIGH);
        prepareNewEvent(trip, request, result);

        assertEquals(result, service.report(COMPANY_ID, request));
        assertEquals(TripStatus.DISRUPTED, trip.getStatus());

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).recordProcessedEvent(
                eq(EVENT_ID), eq(TRIP_ID), hash.capture(), eq(NOW), eq(DISRUPTION_ID)
        );
        assertEquals(64, hash.getValue().length());
    }

    @Test
    void sameEventAndPayloadReturnsOriginalResultWithoutSecondInsert() {
        Trip trip = activeTrip();
        CreateDisruptionRequest request = request(DisruptionSeverity.HIGH);
        DisruptionEvent result = event(DisruptionSeverity.HIGH);
        prepareNewEvent(trip, request, result);
        service.report(COMPANY_ID, request);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).recordProcessedEvent(
                eq(EVENT_ID), eq(TRIP_ID), hash.capture(), eq(NOW), eq(DISRUPTION_ID)
        );
        when(repository.findProcessedEvent(EVENT_ID)).thenReturn(Optional.of(
                new ProcessedEventRecord("DISRUPTION_REPORTED", hash.getValue(), DISRUPTION_ID)
        ));

        assertEquals(result, service.report(COMPANY_ID, request));
        verify(repository, times(1)).insertIfAbsent(request);
    }

    @Test
    void reusedEventIdWithDifferentPayloadIsRejected() {
        Trip trip = activeTrip();
        CreateDisruptionRequest original = request(DisruptionSeverity.HIGH);
        prepareNewEvent(trip, original, event(DisruptionSeverity.HIGH));
        service.report(COMPANY_ID, original);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).recordProcessedEvent(
                eq(EVENT_ID), eq(TRIP_ID), hash.capture(), eq(NOW), eq(DISRUPTION_ID)
        );
        when(repository.findProcessedEvent(EVENT_ID)).thenReturn(Optional.of(
                new ProcessedEventRecord("DISRUPTION_REPORTED", hash.getValue(), DISRUPTION_ID)
        ));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.report(COMPANY_ID, request(DisruptionSeverity.CRITICAL))
        );
        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.getCode());
    }

    @Test
    void rejectsContextSnapshotFromAnotherTrip() {
        Trip trip = activeTrip();
        CreateDisruptionRequest request = request(DisruptionSeverity.HIGH);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));
        when(repository.contextBelongsToTrip(COMPANY_ID, TRIP_ID, CONTEXT_ID)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.report(COMPANY_ID, request)
        );
        assertEquals("CONTEXT_SNAPSHOT_NOT_FOUND", exception.getCode());
        verify(repository, never()).insertIfAbsent(any());
    }

    @Test
    void rejectsDisruptionForTripThatHasNotStarted() {
        Trip trip = Trip.create(
                COMPANY_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TRIP-1", NOW.plusSeconds(3600), 60, 30
        );
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.report(COMPANY_ID, request(DisruptionSeverity.HIGH))
        );
        assertEquals("TRIP_NOT_ACTIVE", exception.getCode());
        verify(repository, never()).contextBelongsToTrip(any(), any(), any());
    }

    private void prepareNewEvent(Trip trip, CreateDisruptionRequest request, DisruptionEvent result) {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));
        when(repository.contextBelongsToTrip(COMPANY_ID, TRIP_ID, CONTEXT_ID)).thenReturn(true);
        when(repository.insertIfAbsent(request)).thenReturn(true);
        when(repository.findByEventId(COMPANY_ID, EVENT_ID)).thenReturn(Optional.of(result));
    }

    private Trip activeTrip() {
        Trip trip = Trip.create(
                COMPANY_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TRIP-1", NOW.plusSeconds(3600), 60, 30
        );
        trip.assign(UUID.randomUUID(), UUID.randomUUID());
        trip.start(NOW.minusSeconds(300));
        return trip;
    }

    private CreateDisruptionRequest request(DisruptionSeverity severity) {
        return new CreateDisruptionRequest(
                EVENT_ID,
                TRIP_ID,
                CONTEXT_ID,
                DisruptionType.VEHICLE_OVERHEATING,
                severity,
                NOW,
                List.of("ENGINE_TEMPERATURE_LIMIT_EXCEEDED"),
                Map.of("engineTemperatureCelsius", 118.5)
        );
    }

    private DisruptionEvent event(DisruptionSeverity severity) {
        return new DisruptionEvent(
                DISRUPTION_ID,
                EVENT_ID,
                TRIP_ID,
                CONTEXT_ID,
                DisruptionType.VEHICLE_OVERHEATING,
                severity,
                NOW,
                true,
                List.of("ENGINE_TEMPERATURE_LIMIT_EXCEEDED"),
                Map.of("engineTemperatureCelsius", 118.5)
        );
    }
}
