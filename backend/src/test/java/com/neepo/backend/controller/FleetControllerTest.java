package com.neepo.backend.controller;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.entity.User;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.UserRepository;
import com.neepo.backend.repository.VehicleRepository;
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
class FleetControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired DriverRepository drivers;
    @Autowired VehicleRepository vehicles;

    @BeforeEach
    void setUp() {
        drivers.deleteAll();
        vehicles.deleteAll();
        users.deleteAll();
    }

    @Test
    void vehicleCreationNormalizesRegistrationAndIsTenantScoped() throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"registrationNumber":" ka 01 ab 1234 ","vehicleType":"Rigid Truck",
                                 "heightMeters":3.8,"widthMeters":2.5,"grossWeightKg":16000,
                                 "payloadCapacityKg":9000,"maximumEngineTemperatureCelsius":110,
                                 "refrigerationSupported":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.registrationNumber").value("KA 01 AB 1234"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).at("/data/id").asText();

        mockMvc.perform(get("/api/v1/vehicles/{id}", id).with(dispatcher(OTHER_COMPANY_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    void driverCreationRequiresSameCompanyDriverUser() throws Exception {
        User driverUser = users.save(User.createForCompany(COMPANY_ID, "Demo Driver", "driver2@neepo.local",
                "+919999999998", UserRole.DRIVER, "unusable"));

        mockMvc.perform(post("/api/v1/drivers").with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"userId":"%s","licenseNumber":" dl-12345 ","maximumDailyDrivingMinutes":540}
                                """.formatted(driverUser.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.licenseNumber").value("DL-12345"))
                .andExpect(jsonPath("$.data.remainingDrivingMinutes").value(540));

        mockMvc.perform(post("/api/v1/drivers").with(dispatcher(OTHER_COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"userId":"%s","licenseNumber":"OTHER","maximumDailyDrivingMinutes":540}
                                """.formatted(driverUser.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DRIVER_USER_NOT_FOUND"));
    }

    @Test
    void driverRoleCannotManageFleet() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/{id}", UUID.randomUUID())
                        .with(jwt().jwt(token -> token.claim("companyId", COMPANY_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor dispatcher(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"));
    }
}
