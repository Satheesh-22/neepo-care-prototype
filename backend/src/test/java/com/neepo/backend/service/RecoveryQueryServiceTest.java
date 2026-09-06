package com.neepo.backend.service;

import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.RecoveryPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecoveryQueryServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DISRUPTION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PLAN_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");

    private TripRepository trips;
    private RecoveryQueryJdbcRepository repository;
    private RecoveryQueryService service;

    @BeforeEach
    void setUp() {
        trips = mock(TripRepository.class);
        repository = mock(RecoveryQueryJdbcRepository.class);
        service = new RecoveryQueryService(trips, repository);
    }

    @Test
    void returnsTenantScopedPlansInRepositoryOrder() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(mock(Trip.class)));
        when(repository.findPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of(mock(RecoveryPlan.class)));

        assertEquals(1, service.listPlans(COMPANY_ID, TRIP_ID).size());
    }

    @Test
    void missingTenantTripCannotQueryRecoveryData() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listPlans(COMPANY_ID, TRIP_ID));

        verifyNoInteractions(repository);
    }

    @Test
    void missingExplanationHasExplicitNotFoundOutcome() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(mock(Trip.class)));
        when(repository.findLatestExplanation(COMPANY_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.latestExplanation(COMPANY_ID, TRIP_ID));
    }

    @Test
    void returnsLatestPersistedExplanation() {
        DecisionExplanation explanation = new DecisionExplanation(
                TRIP_ID, DISRUPTION_ID, PLAN_ID, List.of(), List.of("PLAN_SELECTED_HIGHEST_UTILITY"),
                Map.of(), "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC", Instant.parse("2026-08-24T13:00:00Z")
        );
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(mock(Trip.class)));
        when(repository.findLatestExplanation(COMPANY_ID, TRIP_ID)).thenReturn(Optional.of(explanation));

        assertEquals(explanation, service.latestExplanation(COMPANY_ID, TRIP_ID));
    }
}
