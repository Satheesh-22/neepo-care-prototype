package com.neepo.backend.controller;

import com.neepo.backend.dto.InvalidateRecoveryPlanRequest;
import com.neepo.backend.service.RecoveryInvalidationService;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
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
import java.util.Map;
import java.util.UUID;

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
class RecoveryCommandControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DISRUPTION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PLAN_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID EVENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID TRACE_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant NOW = Instant.parse("2026-08-24T13:00:00Z");

    @Autowired MockMvc mockMvc;
    @MockitoBean RecoveryInvalidationService invalidation;

    @Test
    void dispatcherCanInvalidatePlanAndStartReplanning() throws Exception {
        InvalidateRecoveryPlanRequest request = request();
        when(invalidation.invalidate(eq(COMPANY_ID), eq(TRACE_ID), eq(PLAN_ID), eq(request)))
                .thenReturn(plan());

        mockMvc.perform(post("/api/v1/recovery-plans/{planId}/invalidate", PLAN_ID)
                        .with(role("DISPATCHER"))
                        .header("X-Trace-Id", TRACE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "50000000-0000-0000-0000-000000000005",
                                  "explanationCode": "ROUTE_BLOCKED",
                                  "observedAt": "2026-08-24T13:01:00Z",
                                  "details": "route update"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.id").value(PLAN_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("INVALIDATED"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID.toString()));
    }

    @Test
    void auditorCannotInvalidateRecoveryPlan() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-plans/{planId}/invalidate", PLAN_ID)
                        .with(role("AUDITOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verifyNoInteractions(invalidation);
    }

    @Test
    void invalidationRequestRequiresContractFields() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-plans/{planId}/invalidate", PLAN_ID)
                        .with(role("DISPATCHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor role(String role) {
        return jwt().jwt(token -> token.claim("companyId", COMPANY_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private InvalidateRecoveryPlanRequest request() {
        return new InvalidateRecoveryPlanRequest(EVENT_ID, "ROUTE_BLOCKED", NOW.plusSeconds(60), "route update");
    }

    private RecoveryPlan plan() {
        return new RecoveryPlan(
                PLAN_ID, TRIP_ID, DISRUPTION_ID, RecoveryPlanType.EMERGENCY_ESCALATION,
                RecoveryPlanStatus.INVALIDATED, null, null, List.of(), evaluation(),
                NOW.minusSeconds(30), NOW
        );
    }

    private PlanEvaluation evaluation() {
        return new PlanEvaluation(
                PLAN_ID, true, List.of(), List.of(), Map.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("80.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("80.0000"),
                0.95, Map.of("vehicle.health", "FRESH"), List.of(), NOW,
                "plan-evaluator-1.0.0", "a".repeat(64)
        );
    }
}
