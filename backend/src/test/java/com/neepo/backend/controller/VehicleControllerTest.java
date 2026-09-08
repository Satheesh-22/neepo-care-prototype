package com.neepo.backend.controller;

import com.neepo.backend.domain.VehicleStatus;
import com.neepo.backend.entity.Vehicle;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class VehicleControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-4000-8100-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
    }

    @Test
    void createVehicleWithColdChainSpecs() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles").with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registrationNumber": "NY-7788-REEFER",
                                  "vin": "1HGCR2F8XEA998877",
                                  "make": "Freightliner",
                                  "model": "Cascadia Reefer",
                                  "year": 2025,
                                  "vehicleType": "REFRIGERATED_TRUCK",
                                  "payloadCapacityKg": 9000.0,
                                  "grossWeightKg": 19000.0,
                                  "heightMeters": 3.9,
                                  "widthMeters": 2.5,
                                  "maximumEngineTemperatureCelsius": 105.0,
                                  "refrigerationSupported": true,
                                  "cargoTemperatureMinCelsius": -25.0,
                                  "cargoTemperatureMaxCelsius": 10.0,
                                  "deviceIdentifier": "DEV-TEST-001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationNumber").value("NY-7788-REEFER"))
                .andExpect(jsonPath("$.data.refrigerationSupported").value(true))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    void listVehiclesReturnsFleetList() throws Exception {
        Vehicle vehicle = Vehicle.createDetailed(
                COMPANY_ID, "TX-5544-VAN", "1HGCR2F8XEA443322", "Ford", "Transit", 2024,
                "DRY_VAN", BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(5000.0), BigDecimal.valueOf(3000.0),
                BigDecimal.valueOf(105.0), false, null, null, null, VehicleStatus.AVAILABLE
        );
        vehicleRepository.save(vehicle);

        mockMvc.perform(get("/api/v1/vehicles").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].registrationNumber").value("TX-5544-VAN"))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
    }

    @Test
    void updateVehicleStatus() throws Exception {
        Vehicle vehicle = Vehicle.createDetailed(
                COMPANY_ID, "WA-9900-TRK", "1HGCR2F8XEA112299", "Kenworth", "T680", 2024,
                "HEAVY_TRUCK", BigDecimal.valueOf(4.0), BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(24000.0), BigDecimal.valueOf(12000.0),
                BigDecimal.valueOf(105.0), false, null, null, null, VehicleStatus.AVAILABLE
        );
        vehicle = vehicleRepository.save(vehicle);

        mockMvc.perform(put("/api/v1/vehicles/{id}/status", vehicle.getId()).with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MAINTENANCE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));
    }

    @Test
    void rejectDuplicateRegistrationNumber() throws Exception {
        Vehicle vehicle = Vehicle.createDetailed(
                COMPANY_ID, "DUP-REG-77", "1HGCR2F8XEA112288", "Kenworth", "T680", 2024,
                "HEAVY_TRUCK", BigDecimal.valueOf(4.0), BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(24000.0), BigDecimal.valueOf(12000.0),
                BigDecimal.valueOf(105.0), false, null, null, null, VehicleStatus.AVAILABLE
        );
        vehicleRepository.save(vehicle);

        mockMvc.perform(post("/api/v1/vehicles").with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registrationNumber": "DUP-REG-77",
                                  "vin": "1HGCR2F8XEA998811",
                                  "make": "Freightliner",
                                  "model": "Cascadia",
                                  "year": 2025,
                                  "vehicleType": "HEAVY_TRUCK",
                                  "heightMeters": 4.0,
                                  "widthMeters": 2.5,
                                  "grossWeightKg": 24000.0,
                                  "payloadCapacityKg": 9000.0,
                                  "maximumEngineTemperatureCelsius": 105.0
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_REGISTRATION_EXISTS"));
    }

    @Test
    void driverRoleCannotRegisterVehicle() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles").with(driverJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registrationNumber": "UNAUTH-REG-01",
                                  "vin": "1HGCR2F8XEA998822",
                                  "make": "Freightliner",
                                  "model": "Cascadia",
                                  "year": 2025,
                                  "vehicleType": "HEAVY_TRUCK",
                                  "heightMeters": 4.0,
                                  "widthMeters": 2.5,
                                  "grossWeightKg": 24000.0,
                                  "payloadCapacityKg": 9000.0,
                                  "maximumEngineTemperatureCelsius": 105.0
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(token -> {
            token.claim("companyId", COMPANY_ID.toString());
            token.subject(ADMIN_USER_ID.toString());
            token.claim("role", "ADMIN");
        }).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor driverJwt() {
        return jwt().jwt(token -> {
            token.claim("companyId", COMPANY_ID.toString());
            token.subject(UUID.randomUUID().toString());
            token.claim("role", "DRIVER");
        }).authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }
}
