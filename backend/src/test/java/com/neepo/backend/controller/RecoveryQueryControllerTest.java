package com.neepo.backend.controller;

import com.neepo.backend.service.RecoveryQueryService;
import com.neepo.backend.service.TripReadAccessService;
import com.neepo.contracts.DecisionExplanation;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RecoveryQueryControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DISRUPTION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PLAN_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-08-24T13:00:00Z");

    @Autowired MockMvc mockMvc;
    @MockitoBean RecoveryQueryService recovery;
    @MockitoBean TripReadAccessService readAccess;

    @Test
    void dispatcherCanListRecoveryPlans() throws Exception {
        when(recovery.listPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of(plan()));

        mockMvc.perform(get("/api/v1/trips/{tripId}/recovery-plans", TRIP_ID)
                        .with(role("DISPATCHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(PLAN_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("SELECTED"))
                .andExpect(jsonPath("$.data[0].evaluation.finalUtility").value(80.0));
    }

    @Test
    void dispatcherCanReadLatestDecisionExplanation() throws Exception {
        when(recovery.latestExplanation(COMPANY_ID, TRIP_ID)).thenReturn(explanation());

        mockMvc.perform(get("/api/v1/trips/{tripId}/decision-explanation", TRIP_ID)
                        .with(role("DISPATCHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedPlanId").value(PLAN_ID.toString()))
                .andExpect(jsonPath("$.data.deterministicTieBreakRule")
                        .value("FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC"));
    }

    @Test
    void driverCanReadAssignedRecoveryPlanThroughScopedAccessGuard() throws Exception {
        when(recovery.listPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of(plan()));

        mockMvc.perform(get("/api/v1/trips/{tripId}/recovery-plans", TRIP_ID)
                        .with(role("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SELECTED"));

        verify(readAccess).requireReadAccess(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(TRIP_ID));
    }

    @Test
    void auditorCannotReadOperationalRecoveryData() throws Exception {
        mockMvc.perform(get("/api/v1/trips/{tripId}/recovery-plans", TRIP_ID)
                        .with(role("AUDITOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verifyNoInteractions(recovery);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor role(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                        .claim("companyId", COMPANY_ID.toString())
                        .claim("role", role))
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
                0.95, Map.of("vehicle.health", "FRESH"),
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"), NOW,
                "plan-evaluator-1.0.0", "a".repeat(64)
        );
    }

    private DecisionExplanation explanation() {
        return new DecisionExplanation(
                TRIP_ID, DISRUPTION_ID, PLAN_ID, List.of(),
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"), Map.of(),
                "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC", NOW
        );
    }
}
