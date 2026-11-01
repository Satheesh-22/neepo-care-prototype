package com.neepo.backend.controller;

import com.neepo.backend.entity.FleetAnalyticsSnapshot;
import com.neepo.backend.repository.FleetAnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FleetAnalyticsRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Seed 30d snapshot for COMPANY_ID
        FleetAnalyticsSnapshot snapshot30d = new FleetAnalyticsSnapshot(
                COMPANY_ID,
                "30d",
                68,
                1,
                37,
                21.20,
                96.80,
                148500.00,
                186.00,
                99.40,
                "[{\"cause\":\"Coolant & Thermal Limit Exceeded\",\"count\":18,\"percentage\":47.4,\"averageResolutionMinutes\":17.5}]",
                "[{\"period\":\"Week 1\",\"disruptionCount\":9,\"resolvedCount\":9,\"costAvoidedUsd\":38400.0}]",
                "[{\"strategyName\":\"Safe Parking & Mobile Mechanic\",\"usageCount\":19,\"successRate\":94.7,\"averageCostAvoidanceUsd\":7800.0}]",
                Instant.now()
        );
        repository.save(snapshot30d);

        // Seed 7d snapshot for COMPANY_ID
        FleetAnalyticsSnapshot snapshot7d = new FleetAnalyticsSnapshot(
                COMPANY_ID,
                "7d",
                14,
                1,
                8,
                18.50,
                98.20,
                34200.00,
                42.50,
                100.00,
                "[{\"cause\":\"Coolant & Thermal Limit Exceeded\",\"count\":4,\"percentage\":44.4,\"averageResolutionMinutes\":16.0}]",
                "[{\"period\":\"Day 1\",\"disruptionCount\":1,\"resolvedCount\":1,\"costAvoidedUsd\":4200.0}]",
                "[{\"strategyName\":\"Safe Parking & Mobile Mechanic\",\"usageCount\":4,\"successRate\":100.0,\"averageCostAvoidanceUsd\":8200.0}]",
                Instant.now()
        );
        repository.save(snapshot7d);
    }

    @Test
    void getsFleetAnalyticsWithDefault30dTimeframe() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fleet")
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.data.timeframe").value("30d"))
                .andExpect(jsonPath("$.data.totalTripsMonitored").value(68))
                .andExpect(jsonPath("$.data.activeDisruptionsCount").value(1))
                .andExpect(jsonPath("$.data.resolvedDisruptionsCount").value(37))
                .andExpect(jsonPath("$.data.meanTimeToRecoveryMinutes").value(21.2))
                .andExpect(jsonPath("$.data.driverComplianceRate").value(96.8))
                .andExpect(jsonPath("$.data.estimatedCostAvoidanceUsd").value(148500.0))
                .andExpect(jsonPath("$.data.savedTransitHours").value(186.0))
                .andExpect(jsonPath("$.data.spoilageAvoidanceRate").value(99.4))
                .andExpect(jsonPath("$.data.topDisruptionRootCauses").isArray())
                .andExpect(jsonPath("$.data.topDisruptionRootCauses[0].cause").value("Coolant & Thermal Limit Exceeded"))
                .andExpect(jsonPath("$.data.disruptionTrend").isArray())
                .andExpect(jsonPath("$.data.recoveryStrategyBreakdown").isArray());
    }

    @Test
    void supportsExplicit7dTimeframe() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fleet")
                        .param("timeframe", "7d")
                        .with(admin(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.timeframe").value("7d"))
                .andExpect(jsonPath("$.data.totalTripsMonitored").value(14))
                .andExpect(jsonPath("$.data.estimatedCostAvoidanceUsd").value(34200.0));
    }

    @Test
    void enforcesMultiTenantIsolation() throws Exception {
        // Other company requesting analytics gets clean baseline, not COMPANY_ID's numbers
        mockMvc.perform(get("/api/v1/analytics/fleet")
                        .with(dispatcher(OTHER_COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(OTHER_COMPANY_ID.toString()))
                .andExpect(jsonPath("$.data.totalTripsMonitored").value(0))
                .andExpect(jsonPath("$.data.estimatedCostAvoidanceUsd").value(0.0));
    }

    @Test
    void rejectsDriverRoleWith403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fleet")
                        .with(driver(COMPANY_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidTimeframeWith400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fleet")
                        .param("timeframe", "invalid_timeframe")
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isBadRequest());
    }

    private RequestPostProcessor admin(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor dispatcher(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()).claim("role", "DISPATCHER"))
                .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"));
    }

    private RequestPostProcessor driver(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()).claim("role", "DRIVER"))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }
}
