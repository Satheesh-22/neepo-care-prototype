package com.neepo.backend.service;

import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.TelemetryPolicyDecisionJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.care.telemetry.TelemetryPolicyDecision;
import com.neepo.contracts.TelemetryPolicyState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TelemetryPolicyPersistenceServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID EVENT_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID TRACE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

    private TripRepository trips;
    private TelemetryPolicyDecisionJdbcRepository policies;
    private TelemetryPolicyPersistenceService service;

    @BeforeEach
    void setUp() {
        trips = mock(TripRepository.class);
        policies = mock(TelemetryPolicyDecisionJdbcRepository.class);
        service = new TelemetryPolicyPersistenceService(trips, policies);
    }

    @Test
    void persistsAndAuditsNewDecision() {
        TelemetryPolicyDecision decision = decision(15);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(activeTrip()));
        when(policies.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(policies.insertIfAbsent(decision)).thenReturn(true);

        assertFalse(service.persist(COMPANY_ID, TRACE_ID, decision));

        verify(policies).insertAudit(TRACE_ID, decision);
    }

    @Test
    void exactReplayIsIdempotentAndNotAuditedTwice() {
        TelemetryPolicyDecision decision = decision(15);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(activeTrip()));
        when(policies.findByEventId(EVENT_ID)).thenReturn(Optional.of(decision));

        assertTrue(service.persist(COMPANY_ID, TRACE_ID, decision));

        verify(policies, never()).insertIfAbsent(decision);
        verify(policies, never()).insertAudit(TRACE_ID, decision);
    }

    @Test
    void eventReuseWithDifferentDecisionIsRejected() {
        TelemetryPolicyDecision requested = decision(15);
        TelemetryPolicyDecision existing = decision(60);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(activeTrip()));
        when(policies.findByEventId(EVENT_ID)).thenReturn(Optional.of(existing));

        assertThrows(ResourceConflictException.class, () ->
                service.persist(COMPANY_ID, TRACE_ID, requested));
    }

    @Test
    void missingTenantTripCannotPersistPolicy() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.persist(COMPANY_ID, TRACE_ID, decision(15)));

        verifyNoInteractions(policies);
    }

    @Test
    void inactiveTripCannotPersistPolicy() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(baseTrip()));

        assertThrows(ResourceConflictException.class, () ->
                service.persist(COMPANY_ID, TRACE_ID, decision(15)));

        verifyNoInteractions(policies);
    }

    private TelemetryPolicyDecision decision(int interval) {
        return new TelemetryPolicyDecision(
                TRIP_ID, EVENT_ID, TelemetryPolicyState.NEAR_DECISION_POINT,
                interval, NOW, TelemetryPolicyState.NEAR_DECISION_POINT.name()
        );
    }

    private Trip activeTrip() {
        Trip trip = baseTrip();
        trip.assign(UUID.randomUUID(), UUID.randomUUID());
        trip.start(NOW.minusSeconds(300));
        return trip;
    }

    private Trip baseTrip() {
        return Trip.create(
                COMPANY_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TRIP-POLICY-1", NOW.plusSeconds(3600), 60, 30
        );
    }
}
