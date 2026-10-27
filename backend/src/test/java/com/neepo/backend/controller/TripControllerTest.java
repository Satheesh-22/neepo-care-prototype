package com.neepo.backend.controller;

import com.neepo.backend.domain.CargoType;
import com.neepo.backend.domain.DriverStatus;
import com.neepo.backend.domain.VehicleStatus;
import com.neepo.backend.entity.Cargo;
import com.neepo.backend.entity.Driver;
import com.neepo.backend.entity.RouteReference;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.entity.Vehicle;
import com.neepo.backend.repository.*;
import com.neepo.backend.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TripControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired TripAssignmentRepository assignments;
    @Autowired TripRepository trips;
    @Autowired RouteReferenceRepository routes;
    @Autowired CargoRepository cargo;
    @Autowired DriverRepository drivers;
    @Autowired VehicleRepository vehicles;
    @MockitoBean TelemetryService telemetry;

    private Vehicle vehicle;
    private Driver driver;
    private Cargo shipment;
    private RouteReference route;

    @BeforeEach
    void setUp() {
        assignments.deleteAll();
        trips.deleteAll();
        routes.deleteAll();
        cargo.deleteAll();
        drivers.deleteAll();
        vehicles.deleteAll();
        vehicle = vehicles.save(Vehicle.create(COMPANY_ID, "KA-01-TRIP", "Rigid Truck",
                new BigDecimal("3.8"), new BigDecimal("2.5"), new BigDecimal("16000"),
                new BigDecimal("9000"), new BigDecimal("110"), false));
        driver = drivers.save(Driver.create(COMPANY_ID, UUID.randomUUID(), "DL-TRIP", 540));
        shipment = cargo.save(Cargo.create(COMPANY_ID, "SHIP-001", CargoType.GENERAL, "Pallets",
                new BigDecimal("1200"), null, null, false));
        route = routes.save(RouteReference.reference(UUID.randomUUID(), COMPANY_ID));
    }

    @Test
    void tripMovesDeterministicallyFromCreatedToAssignedToInProgress() throws Exception {
        String created = mockMvc.perform(post("/api/v1/trips").with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"referenceCode":" trip-001 ","vehicleId":"%s","driverId":"%s",
                                 "cargoId":"%s","routeId":"%s","deliveryDeadline":"%s",
                                 "maximumWaitingMinutes":60,"acceptedEtaThresholdMinutes":30}
                                """.formatted(vehicle.getId(), driver.getId(), shipment.getId(), route.getId(),
                                Instant.now().plus(2, ChronoUnit.DAYS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceCode").value("TRIP-001"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn().getResponse().getContentAsString();
        String tripId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).at("/data/id").asText();

        mockMvc.perform(post("/api/v1/trips/{id}/start", tripId).with(dispatcher(COMPANY_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_STARTABLE"));

        mockMvc.perform(post("/api/v1/trips/{id}/assign", tripId).with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"vehicleId":"%s","driverId":"%s","assignedAt":"%s"}
                                """.formatted(vehicle.getId(), driver.getId(), Instant.now().minusSeconds(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        mockMvc.perform(post("/api/v1/trips/{id}/start", tripId).with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty());

        assertEquals(VehicleStatus.IN_TRANSIT, vehicles.findById(vehicle.getId()).orElseThrow().getStatus());
        assertEquals(DriverStatus.DRIVING, drivers.findById(driver.getId()).orElseThrow().getStatus());
    }

    @Test
    void tripCannotBeReadAcrossCompanyBoundary() throws Exception {
        String created = mockMvc.perform(post("/api/v1/trips").with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"referenceCode":"TRIP-PRIVATE","vehicleId":"%s","driverId":"%s",
                                 "cargoId":"%s","routeId":"%s","deliveryDeadline":"%s",
                                 "maximumWaitingMinutes":60,"acceptedEtaThresholdMinutes":30}
                                """.formatted(vehicle.getId(), driver.getId(), shipment.getId(), route.getId(),
                                Instant.now().plus(2, ChronoUnit.DAYS))))
                .andReturn().getResponse().getContentAsString();
        String tripId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).at("/data/id").asText();

        mockMvc.perform(get("/api/v1/trips/{id}", tripId).with(dispatcher(OTHER_COMPANY_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void driverCanReadOnlyTheAssignedTrip() throws Exception {
        Trip assignedTrip = trips.save(Trip.create(
                COMPANY_ID, vehicle.getId(), driver.getId(), shipment.getId(), route.getId(),
                "TRIP-DRIVER-ASSIGNED", Instant.now().plus(2, ChronoUnit.DAYS), 60, 30
        ));

        mockMvc.perform(get("/api/v1/trips/{id}", assignedTrip.getId())
                        .with(driver(driver.getUserId(), COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignedTrip.getId().toString()));

        Driver otherDriver = drivers.save(Driver.create(
                COMPANY_ID, UUID.randomUUID(), "DL-OTHER", 540
        ));
        mockMvc.perform(get("/api/v1/trips/{id}", assignedTrip.getId())
                        .with(driver(otherDriver.getUserId(), COMPANY_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void driverCannotCreateTrips() throws Exception {
        mockMvc.perform(post("/api/v1/trips")
                        .with(driver(driver.getUserId(), COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void canRetrieveTelemetryHistoryForAuthorizedTrip() throws Exception {
        Trip assignedTrip = trips.save(Trip.create(
                COMPANY_ID, vehicle.getId(), driver.getId(), shipment.getId(), route.getId(),
                "TRIP-HISTORY-TEST", Instant.now().plus(2, ChronoUnit.DAYS), 60, 30
        ));

        when(telemetry.getHistory(eq(assignedTrip.getId()), any(), any(), eq(500)))
                .thenReturn(List.of(
                        new com.neepo.backend.dto.TelemetryHistoryItemResponse(
                                UUID.randomUUID(), Instant.now(), new BigDecimal("13.0358"), new BigDecimal("77.5970"),
                                new BigDecimal("106.2"), new BigDecimal("4.0"), new BigDecimal("0.0"),
                                new BigDecimal("0.98"), com.neepo.backend.domain.ObservationStatus.FRESH,
                                com.neepo.backend.domain.TelemetrySource.VEHICLE_DEVICE, com.neepo.backend.domain.NetworkState.ONLINE
                        )
                ));

        mockMvc.perform(get("/api/v1/trips/{id}/telemetry/history", assignedTrip.getId())
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].engineTemperatureCelsius").value(106.2));

        mockMvc.perform(get("/api/v1/trips/{id}/telemetry/history", assignedTrip.getId())
                        .with(driver(driver.getUserId(), COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/trips/{id}/telemetry/history", assignedTrip.getId())
                        .with(dispatcher(OTHER_COMPANY_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_FOUND"));

        Driver otherDriver = drivers.save(Driver.create(
                COMPANY_ID, UUID.randomUUID(), "DL-HIST-OTHER", 540
        ));
        mockMvc.perform(get("/api/v1/trips/{id}/telemetry/history", assignedTrip.getId())
                        .with(driver(otherDriver.getUserId(), COMPANY_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_FOUND"));
    }


    private org.springframework.test.web.servlet.request.RequestPostProcessor dispatcher(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor driver(UUID userId, UUID companyId) {
        return jwt().jwt(token -> token.subject(userId.toString())
                        .claim("companyId", companyId.toString())
                        .claim("role", "DRIVER"))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }
}
