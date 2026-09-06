package com.neepo.backend.controller;

import com.neepo.backend.domain.ProviderAvailabilityStatus;
import com.neepo.backend.domain.ProviderType;
import com.neepo.backend.dto.GeoPointResponse;
import com.neepo.backend.dto.ProviderAvailabilityRequest;
import com.neepo.backend.dto.ProviderAvailabilityResponse;
import com.neepo.backend.dto.ProviderResponse;
import com.neepo.backend.service.ProviderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ProviderControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID PROVIDER_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final Instant OBSERVED_AT = Instant.parse("2026-08-17T12:00:00Z");

    @Autowired MockMvc mockMvc;
    @MockitoBean ProviderService providers;

    @Test
    void listsAccessibleProvidersForDispatcher() throws Exception {
        when(providers.list(COMPANY_ID, 0, 20)).thenReturn(List.of(provider()));

        mockMvc.perform(get("/api/v1/providers").with(dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(PROVIDER_ID.toString()))
                .andExpect(jsonPath("$.data[0].location.latitude").value(12.9716))
                .andExpect(jsonPath("$.data[0].capabilities[0]").value("ENGINE_DIAGNOSTICS"))
                .andExpect(jsonPath("$.data[0].latestAvailability.status").value("AVAILABLE"));
    }

    @Test
    void deniesProviderOperationsToDriver() throws Exception {
        mockMvc.perform(get("/api/v1/providers").with(jwt()
                        .jwt(token -> token.claim("companyId", COMPANY_ID.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verifyNoInteractions(providers);
    }

    @Test
    void recordsValidAvailabilityObservation() throws Exception {
        when(providers.recordAvailability(eq(COMPANY_ID), eq(PROVIDER_ID), any(ProviderAvailabilityRequest.class)))
                .thenReturn(provider());

        mockMvc.perform(post("/api/v1/providers/{providerId}/availability", PROVIDER_ID)
                        .with(dispatcher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"AVAILABLE","observedAt":"2026-08-17T12:00:00Z",
                                 "expiresAt":"2026-08-17T12:15:00Z","estimatedArrivalMinutes":25,
                                 "confidence":0.95,"requiredResourceAvailable":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.latestAvailability.estimatedArrivalMinutes").value(25));
    }

    @Test
    void rejectsAvailabilityThatExpiresBeforeItWasObserved() throws Exception {
        mockMvc.perform(post("/api/v1/providers/{providerId}/availability", PROVIDER_ID)
                        .with(dispatcher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"AVAILABLE","observedAt":"2026-08-17T12:00:00Z",
                                 "expiresAt":"2026-08-17T11:59:00Z","confidence":0.95,
                                 "requiredResourceAvailable":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(providers);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor dispatcher() {
        return jwt().jwt(token -> token.claim("companyId", COMPANY_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"));
    }

    private ProviderResponse provider() {
        return new ProviderResponse(
                PROVIDER_ID,
                "Bengaluru Mobile Recovery",
                ProviderType.MOBILE_MECHANIC,
                new GeoPointResponse(new BigDecimal("12.9716"), new BigDecimal("77.5946")),
                new BigDecimal("80.00"),
                new BigDecimal("96.50"),
                true,
                List.of("ENGINE_DIAGNOSTICS", "REFRIGERATION_REPAIR"),
                new ProviderAvailabilityResponse(
                        ProviderAvailabilityStatus.AVAILABLE,
                        OBSERVED_AT,
                        OBSERVED_AT.plusSeconds(900),
                        25,
                        new BigDecimal("0.950"),
                        true
                )
        );
    }
}
