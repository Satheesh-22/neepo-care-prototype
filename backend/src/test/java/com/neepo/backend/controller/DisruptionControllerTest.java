package com.neepo.backend.controller;

import com.neepo.backend.dto.CreateDisruptionRequest;
import com.neepo.backend.service.DisruptionService;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DisruptionControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID CONTEXT_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID EVENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");
    private static final UUID DISRUPTION_ID = UUID.fromString("30000000-0000-4000-8000-000000000004");
    private static final Instant DETECTED_AT = Instant.parse("2026-08-17T12:00:00Z");

    @Autowired MockMvc mockMvc;
    @MockitoBean DisruptionService disruptions;

    @Test
    void driverCanReportDisruption() throws Exception {
        when(disruptions.report(eq(COMPANY_ID), any(CreateDisruptionRequest.class))).thenReturn(event());

        mockMvc.perform(post("/api/v1/disruptions")
                        .with(role("DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.id").value(DISRUPTION_ID.toString()))
                .andExpect(jsonPath("$.data.type").value("VEHICLE_OVERHEATING"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.evidence.engineTemperatureCelsius").value(118.5));
    }

    @Test
    void rejectsIncompleteDisruptionEvidence() throws Exception {
        mockMvc.perform(post("/api/v1/disruptions")
                        .with(role("DISPATCHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId":"30000000-0000-4000-8000-000000000003",
                                 "tripId":"30000000-0000-4000-8000-000000000001",
                                 "contextSnapshotId":"30000000-0000-4000-8000-000000000002"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(disruptions);
    }

    @Test
    void authenticatedUserWithoutOperationalRoleIsDenied() throws Exception {
        mockMvc.perform(post("/api/v1/disruptions")
                        .with(role("AUDITOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verifyNoInteractions(disruptions);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor role(String role) {
        return jwt().jwt(token -> token.claim("companyId", COMPANY_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String validRequest() {
        return """
                {"eventId":"30000000-0000-4000-8000-000000000003",
                 "tripId":"30000000-0000-4000-8000-000000000001",
                 "contextSnapshotId":"30000000-0000-4000-8000-000000000002",
                 "type":"VEHICLE_OVERHEATING","severity":"HIGH",
                 "detectedAt":"2026-08-17T12:00:00Z",
                 "explanationCodes":["ENGINE_TEMPERATURE_LIMIT_EXCEEDED"],
                 "evidence":{"engineTemperatureCelsius":118.5}}
                """;
    }

    private DisruptionEvent event() {
        return new DisruptionEvent(
                DISRUPTION_ID,
                EVENT_ID,
                TRIP_ID,
                CONTEXT_ID,
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                DETECTED_AT,
                true,
                List.of("ENGINE_TEMPERATURE_LIMIT_EXCEEDED"),
                Map.of("engineTemperatureCelsius", 118.5)
        );
    }
}
