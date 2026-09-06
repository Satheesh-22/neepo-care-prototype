package com.neepo.backend.controller;

import com.neepo.backend.repository.CargoRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CargoControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired CargoRepository cargo;

    @BeforeEach void setUp() { cargo.deleteAll(); }

    @Test
    void createsTemperatureControlledCargoAndProtectsTenantBoundary() throws Exception {
        String response = mockMvc.perform(post("/api/v1/cargo").with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"referenceCode":" cold-001 ","cargoType":"REFRIGERATED",
                                 "description":"Vaccines","weightKg":800,
                                 "minimumTemperatureCelsius":2,"maximumTemperatureCelsius":8,
                                 "hazardous":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceCode").value("COLD-001"))
                .andExpect(jsonPath("$.data.cargoType").value("REFRIGERATED"))
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).at("/data/id").asText();

        mockMvc.perform(get("/api/v1/cargo/{id}", id).with(dispatcher(OTHER_COMPANY_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CARGO_NOT_FOUND"));
    }

    @Test
    void rejectsInvertedTemperatureRange() throws Exception {
        mockMvc.perform(post("/api/v1/cargo").with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"referenceCode":"BAD-TEMP","cargoType":"FROZEN","description":"Frozen food",
                                 "weightKg":500,"minimumTemperatureCelsius":-10,
                                 "maximumTemperatureCelsius":-20,"hazardous":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor dispatcher(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"));
    }
}
