package com.neepo.backend.service;

import com.neepo.backend.domain.NetworkState;
import com.neepo.backend.domain.ObservationStatus;
import com.neepo.backend.domain.TelemetrySource;
import com.neepo.backend.dto.GeoPointRequest;
import com.neepo.backend.dto.TelemetryObservationRequest;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.repository.TelemetryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelemetryServiceTest {
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");

    @Test
    void retryOfSameEventIsReportedAsDuplicate() {
        TripRepository trips = mock(TripRepository.class);
        TelemetryJdbcRepository telemetry = mock(TelemetryJdbcRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        Trip trip = Trip.create(COMPANY_ID, VEHICLE_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TRIP-1", NOW.plusSeconds(3600), 60, 30);
        TelemetryObservationRequest observation = observation(VEHICLE_ID);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));
        when(telemetry.insertIfAbsent(observation)).thenReturn(false);

        var result = new TelemetryService(trips, telemetry, Clock.fixed(NOW, ZoneOffset.UTC), events)
                .ingest(COMPANY_ID, observation);

        assertTrue(result.duplicate());
        assertEquals(NOW, result.acceptedAt());
        verifyNoInteractions(events);
    }

    @Test
    void rejectsTelemetryFromVehicleNotAssignedToTrip() {
        TripRepository trips = mock(TripRepository.class);
        TelemetryJdbcRepository telemetry = mock(TelemetryJdbcRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        Trip trip = Trip.create(COMPANY_ID, VEHICLE_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TRIP-1", NOW.plusSeconds(3600), 60, 30);
        TelemetryObservationRequest observation = observation(UUID.randomUUID());
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));

        assertThrows(ResourceConflictException.class,
                () -> new TelemetryService(trips, telemetry, Clock.fixed(NOW, ZoneOffset.UTC), events)
                        .ingest(COMPANY_ID, observation));
        verifyNoInteractions(telemetry);
        verifyNoInteractions(events);
    }

    @Test
    void newTelemetryPublishesAnAfterCommitEvent() {
        TripRepository trips = mock(TripRepository.class);
        TelemetryJdbcRepository telemetry = mock(TelemetryJdbcRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        Trip trip = Trip.create(COMPANY_ID, VEHICLE_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TRIP-1", NOW.plusSeconds(3600), 60, 30);
        TelemetryObservationRequest observation = observation(VEHICLE_ID);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));
        when(telemetry.insertIfAbsent(observation)).thenReturn(true);

        new TelemetryService(trips, telemetry, Clock.fixed(NOW, ZoneOffset.UTC), events)
                .ingest(COMPANY_ID, observation);

        verify(events).publishEvent(argThat((Object event) -> event instanceof TelemetryUpdatedEvent updated
                && updated.observation().equals(observation)
                && updated.occurredAt().equals(NOW)));
    }

    private TelemetryObservationRequest observation(UUID vehicleId) {
        return new TelemetryObservationRequest(UUID.randomUUID(), TRIP_ID, vehicleId,
                NOW.minusSeconds(5), NOW, NOW.plusSeconds(60),
                new GeoPointRequest(new BigDecimal("12.9716"), new BigDecimal("77.5946")),
                new BigDecimal("95"), null, new BigDecimal("45"), new BigDecimal("0.99"),
                ObservationStatus.FRESH, TelemetrySource.VEHICLE_DEVICE, NetworkState.ONLINE);
    }
}
