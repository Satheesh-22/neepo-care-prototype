package com.neepo.backend.controller;

import com.neepo.backend.dto.TelemetryPolicyRequest;
import com.neepo.backend.service.TelemetryPolicyEvaluationService;
import com.neepo.backend.service.TripReadAccessService;
import com.neepo.care.telemetry.TelemetryPolicyDecision;
import com.neepo.contracts.TelemetryPolicyState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TelemetryPolicyControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID EVENT_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Autowired MockMvc mockMvc;
    @MockitoBean TelemetryPolicyEvaluationService policies;
    @MockitoBean TripReadAccessService readAccess;

    @Test
    void assignedDriverCanConfirmSafelyParkedPolicy() throws Exception {
        when(policies.evaluate(eq(COMPANY_ID), eq(TRIP_ID), any(), any(TelemetryPolicyRequest.class)))
                .thenReturn(new TelemetryPolicyDecision(
                        TRIP_ID, EVENT_ID, TelemetryPolicyState.SAFELY_PARKED,
                        120, NOW, "SAFELY_PARKED"
                ));

        mockMvc.perform(post("/api/v1/trips/{tripId}/telemetry-policy", TRIP_ID)
                        .with(role("DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "observedAt": "%s",
                                  "nearDecisionPoint": false,
                                  "safelyParked": true
                                }
                                """.formatted(EVENT_ID, NOW)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyState").value("SAFELY_PARKED"))
                .andExpect(jsonPath("$.data.intervalSeconds").value(120));

        verify(readAccess).requireReadAccess(any(), eq(TRIP_ID));
    }

    @Test
    void invalidPolicyRequestIsRejectedBeforeEvaluation() throws Exception {
        mockMvc.perform(post("/api/v1/trips/{tripId}/telemetry-policy", TRIP_ID)
                        .with(role("DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nearDecisionPoint\":false,\"safelyParked\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(policies);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor role(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                        .claim("companyId", COMPANY_ID.toString())
                        .claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
