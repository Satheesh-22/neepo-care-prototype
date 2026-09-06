package com.neepo.backend.service;

import com.neepo.backend.domain.ProviderAvailabilityStatus;
import com.neepo.backend.domain.ProviderType;
import com.neepo.backend.dto.GeoPointResponse;
import com.neepo.backend.dto.ProviderAvailabilityRequest;
import com.neepo.backend.dto.ProviderAvailabilityResponse;
import com.neepo.backend.dto.ProviderResponse;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.ProviderJdbcRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderServiceTest {
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID PROVIDER_ID = UUID.randomUUID();
    private static final Instant OBSERVED_AT = Instant.parse("2026-08-17T12:00:00Z");

    private final ProviderJdbcRepository repository = mock(ProviderJdbcRepository.class);
    private final ProviderService service = new ProviderService(repository);

    @Test
    void listsCompanyAndSharedProvidersUsingRequestedPage() {
        ProviderResponse provider = provider(null);
        when(repository.findAccessible(COMPANY_ID, 2, 25)).thenReturn(List.of(provider));

        assertEquals(List.of(provider), service.list(COMPANY_ID, 2, 25));
        verify(repository).findAccessible(COMPANY_ID, 2, 25);
    }

    @Test
    void appendsAvailabilityAndReturnsLatestProviderState() {
        ProviderAvailabilityRequest availability = availability();
        ProviderResponse provider = provider(availability);
        when(repository.appendAvailability(COMPANY_ID, PROVIDER_ID, availability)).thenReturn(true);
        when(repository.findAccessibleById(COMPANY_ID, PROVIDER_ID)).thenReturn(Optional.of(provider));

        assertEquals(provider, service.recordAvailability(COMPANY_ID, PROVIDER_ID, availability));
        verify(repository).appendAvailability(COMPANY_ID, PROVIDER_ID, availability);
    }

    @Test
    void concealsProviderOutsideTenantScope() {
        ProviderAvailabilityRequest availability = availability();
        when(repository.appendAvailability(COMPANY_ID, PROVIDER_ID, availability)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.recordAvailability(COMPANY_ID, PROVIDER_ID, availability)
        );

        assertEquals("PROVIDER_NOT_FOUND", exception.getCode());
        verify(repository, never()).findAccessibleById(COMPANY_ID, PROVIDER_ID);
    }

    private ProviderAvailabilityRequest availability() {
        return new ProviderAvailabilityRequest(
                ProviderAvailabilityStatus.AVAILABLE,
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(900),
                25,
                new BigDecimal("0.950"),
                true
        );
    }

    private ProviderResponse provider(ProviderAvailabilityRequest request) {
        return new ProviderResponse(
                PROVIDER_ID,
                "Bengaluru Mobile Recovery",
                ProviderType.MOBILE_MECHANIC,
                new GeoPointResponse(new BigDecimal("12.9716"), new BigDecimal("77.5946")),
                new BigDecimal("80.00"),
                new BigDecimal("96.50"),
                true,
                List.of("ENGINE_DIAGNOSTICS", "REFRIGERATION_REPAIR"),
                request == null ? null : new ProviderAvailabilityResponse(
                        request.status(), request.observedAt(), request.expiresAt(),
                        request.estimatedArrivalMinutes(), request.confidence(),
                        request.requiredResourceAvailable()
                )
        );
    }
}
