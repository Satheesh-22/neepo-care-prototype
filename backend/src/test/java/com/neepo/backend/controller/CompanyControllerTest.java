package com.neepo.backend.controller;

import com.neepo.backend.entity.Company;
import com.neepo.backend.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CompanyControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        companyRepository.deleteAll();
        Company company = Company.create(
                "NEEPO Cold Chain", "NEEPO-DEMO", "NEEPO Cold Chain Logistics Inc.",
                "NEEPO Cold Chain", "REG-NEEPO-DEMO", "Cold Chain Logistics",
                "100 Market St", "San Francisco", "CA", "US", "94105",
                "Operations Director", "admin@neepo.local", "+15550100000",
                "America/Los_Angeles", "METRIC", "STRICT_COLD_CHAIN", null, 90
        );
        companyRepository.save(company);
    }

    @Test
    void getMyCompanyReturnsCurrentProfile() throws Exception {
        Company saved = companyRepository.findAll().get(0);
        mockMvc.perform(get("/api/v1/company").with(jwtForCompany(saved.getId(), "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("NEEPO-DEMO"));
    }

    @Test
    void updateMyCompanyUpdatesFieldsSuccessfully() throws Exception {
        Company saved = companyRepository.findAll().get(0);
        mockMvc.perform(put("/api/v1/company").with(jwtForCompany(saved.getId(), "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "Updated Cold Chain Inc.",
                                  "tradingName": "Updated Cold Chain Corp",
                                  "registrationNumber": "REG-NEEPO-DEMO",
                                  "businessEmail": "billing@updated.com",
                                  "phoneNumber": "+15550199999",
                                  "country": "US",
                                  "timezone": "America/New_York",
                                  "measurementUnits": "METRIC",
                                  "cargoTemperaturePolicy": "STRICT_COLD_CHAIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.legalName").value("Updated Cold Chain Inc."))
                .andExpect(jsonPath("$.data.businessEmail").value("billing@updated.com"));
    }

    @Test
    void nonAdminCannotUpdateCompanyProfile() throws Exception {
        Company saved = companyRepository.findAll().get(0);
        mockMvc.perform(put("/api/v1/company").with(jwtForCompany(saved.getId(), "ROLE_DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "Malicious Update",
                                  "registrationNumber": "REG-MALICIOUS",
                                  "businessEmail": "hacker@test.com"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtForCompany(UUID companyId, String role) {
        return jwt().jwt(token -> {
            token.claim("companyId", companyId.toString());
            token.claim("sub", UUID.randomUUID().toString());
            token.claim("role", role.replace("ROLE_", ""));
        }).authorities(new SimpleGrantedAuthority(role));
    }
}
