package com.neepo.backend.controller;

import com.neepo.backend.service.RecoveryPlanGenerationService;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RecoveryGenerationControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DISRUPTION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PLAN_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID TRACE_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant NOW = Instant.parse("2026-08-24T13:00:00Z");

    @Autowired MockMvc mockMvc;
    @MockitoBean RecoveryPlanGenerationService generation;

    @Test
    void dispatcherCanGenerateRecoveryPlans() throws Exception {
        when(generation.generate(COMPANY_ID, TRIP_ID, TRACE_ID)).thenReturn(List.of(plan()));

        mockMvc.perform(post("/api/v1/trips/{tripId}/recovery-plans/generate", TRIP_ID)
                        .with(role("DISPATCHER"))
                        .header("X-Trace-Id", TRACE_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data[0].id").value(PLAN_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("SELECTED"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID.toString()));
    }

    @Test
    void adminCanGenerateRecoveryPlans() throws Exception {
        when(generation.generate(COMPANY_ID, TRIP_ID, TRACE_ID)).thenReturn(List.of(plan()));

        mockMvc.perform(post("/api/v1/trips/{tripId}/recovery-plans/generate", TRIP_ID)
                        .with(role("ADMIN"))
                        .header("X-Trace-Id", TRACE_ID))
                .andExpect(status().isAccepted());
    }

    @Test
    void auditorCannotGenerateRecoveryPlans() throws Exception {
        mockMvc.perform(post("/api/v1/trips/{tripId}/recovery-plans/generate", TRIP_ID)
                        .with(role("AUDITOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verifyNoInteractions(generation);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor role(String role) {
        return jwt().jwt(token -> token.claim("companyId", COMPANY_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private RecoveryPlan plan() {
        return new RecoveryPlan(
                PLAN_ID, TRIP_ID, DISRUPTION_ID, RecoveryPlanType.EMERGENCY_ESCALATION,
                RecoveryPlanStatus.SELECTED, null, null, List.of(), evaluation(),
                NOW.minusSeconds(10), NOW
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
