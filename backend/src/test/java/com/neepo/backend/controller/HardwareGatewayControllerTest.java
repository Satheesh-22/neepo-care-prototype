package com.neepo.backend.controller;

import com.neepo.backend.entity.HardwareGateway;
import com.neepo.backend.repository.HardwareGatewayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class HardwareGatewayControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-4000-8300-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("00000000-0000-4000-8600-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HardwareGatewayRepository repository;

    @Autowired
    private com.neepo.backend.repository.TripRepository tripRepository;

    @Autowired
    private com.neepo.backend.repository.CargoRepository cargoRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.neepo.backend.repository.TelemetryJdbcRepository telemetryRepository;

    private HardwareGateway gateway;
    private com.neepo.backend.entity.Trip testTrip;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        com.neepo.backend.entity.Cargo cargo = com.neepo.backend.entity.Cargo.create(COMPANY_ID, "CARGO-GW-" + UUID.randomUUID().toString().substring(0, 8),
                com.neepo.backend.domain.CargoType.REFRIGERATED, "Biologics", java.math.BigDecimal.valueOf(500),
                java.math.BigDecimal.valueOf(2.0), java.math.BigDecimal.valueOf(8.0), false);
        cargo = cargoRepository.save(cargo);

        testTrip = com.neepo.backend.entity.Trip.create(COMPANY_ID, VEHICLE_ID, UUID.randomUUID(), cargo.getId(),
                UUID.randomUUID(), "TRIP-GW-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now().plusSeconds(3600), 60, 30);
        testTrip = tripRepository.save(testTrip);

        gateway = new HardwareGateway(COMPANY_ID, "NEEPO-TEST-GW-001", "test_hmac_secret_key_123");
        gateway.setVehicleId(VEHICLE_ID);
        gateway.setImei("867530901234567");
        gateway.setSecureElementId("ATECC608A-TEST-001");
        gateway.setFirmwareVersion("v1.1.0-prod");
        gateway.setStatus("ACTIVE");
        gateway.setLastHeartbeatAt(Instant.now());
        gateway.setBufferCapacityRecords(10000);
        gateway.setReportingIntervalSeconds(5);
        gateway.setMetadata("{\"mcu\":\"ESP32-S3\"}");
        gateway = repository.save(gateway);
    }

    private RequestPostProcessor dispatcherAuth() {
        return jwt().jwt(jwt -> jwt
                .claim("companyId", COMPANY_ID.toString())
                .claim("sub", "dispatcher-1")
        ).authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"));
    }

    private RequestPostProcessor otherCompanyAuth() {
        return jwt().jwt(jwt -> jwt
                .claim("companyId", OTHER_COMPANY_ID.toString())
                .claim("sub", "other-admin")
        ).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void listGateways_returnsOkForDispatcher() throws Exception {
        mockMvc.perform(get("/api/v1/gateways").with(dispatcherAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].deviceSerial").value("NEEPO-TEST-GW-001"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].bufferCapacityRecords").value(10000));
    }

    @Test
    void getGateway_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/gateways/" + gateway.getId()).with(dispatcherAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gateway.getId().toString()))
                .andExpect(jsonPath("$.data.deviceSerial").value("NEEPO-TEST-GW-001"))
                .andExpect(jsonPath("$.data.firmwareVersion").value("v1.1.0-prod"));
    }

    @Test
    void registerGateway_createsNewRecord() throws Exception {
        String body = """
                {
                    "deviceSerial": "NEEPO-GW-NEW-999",
                    "imei": "867530909999999",
                    "secureElementId": "ATECC608A-NEW-999",
                    "firmwareVersion": "v1.1.0-prod",
                    "hmacSecret": "brand_new_secret_key_456",
                    "bufferCapacityRecords": 10000,
                    "reportingIntervalSeconds": 3,
                    "metadata": { "cellular": "4G LTE", "antenna": "external" }
                }
                """;

        mockMvc.perform(post("/api/v1/gateways")
                        .with(dispatcherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceSerial").value("NEEPO-GW-NEW-999"))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    @Test
    void getOtaConfig_returnsPolicyAndThresholds() throws Exception {
        mockMvc.perform(get("/api/v1/gateways/" + gateway.getId() + "/config").with(dispatcherAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gatewayId").value(gateway.getId().toString()))
                .andExpect(jsonPath("$.data.deviceSerial").value("NEEPO-TEST-GW-001"))
                .andExpect(jsonPath("$.data.reportingIntervalSeconds").value(5))
                .andExpect(jsonPath("$.data.ingestionEndpoint").value("/api/v1/telemetry"));
    }

    @Test
    void recordHeartbeat_updatesTimestamp() throws Exception {
        String body = """
                {
                    "firmwareVersion": "v1.1.1-patch",
                    "batteryVoltage": 12.6,
                    "signalRssi": -65,
                    "bufferedRecordsCount": 0
                }
                """;

        mockMvc.perform(post("/api/v1/gateways/" + gateway.getId() + "/heartbeat")
                        .with(dispatcherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.otaUpdateAvailable").value(false));
    }

    @Test
    void syncBatch_deduplicatesRecords() throws Exception {
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        Instant now = Instant.now();

        String body = String.format("""
                {
                    "tripId": "%s",
                    "syncMode": "OFFLINE_REPLAY",
                    "networkCarrier": "Verizon Telematics",
                    "signalRssi": -72,
                    "records": [
                        {
                            "eventId": "%s",
                            "tripId": "%s",
                            "vehicleId": "%s",
                            "recordedAt": "%s",
                            "receivedAt": "%s",
                            "expiresAt": "%s",
                            "location": { "latitude": 11.42345, "longitude": 77.69123 },
                            "engineTemperatureCelsius": 94.5,
                            "cargoTemperatureCelsius": 4.5,
                            "speedKph": 52.0,
                            "confidence": 0.99,
                            "observationStatus": "FRESH",
                            "source": "VEHICLE_DEVICE",
                            "networkState": "SYNCHRONIZING"
                        },
                        {
                            "eventId": "%s",
                            "tripId": "%s",
                            "vehicleId": "%s",
                            "recordedAt": "%s",
                            "receivedAt": "%s",
                            "expiresAt": "%s",
                            "location": { "latitude": 11.42500, "longitude": 77.69250 },
                            "engineTemperatureCelsius": 95.0,
                            "cargoTemperatureCelsius": 4.6,
                            "speedKph": 54.0,
                            "confidence": 0.99,
                            "observationStatus": "FRESH",
                            "source": "VEHICLE_DEVICE",
                            "networkState": "SYNCHRONIZING"
                        }
                    ]
                }
                """,
                testTrip.getId(),
                eventId1, testTrip.getId(), VEHICLE_ID, now, now.plusMillis(100), now.plusSeconds(300),
                eventId2, testTrip.getId(), VEHICLE_ID, now.plusSeconds(5), now.plusSeconds(5).plusMillis(100), now.plusSeconds(305)
        );

        org.mockito.Mockito.when(telemetryRepository.insertIfAbsent(org.mockito.ArgumentMatchers.any()))
                .thenReturn(true, true, false, false);

        // First sync
        mockMvc.perform(post("/api/v1/gateways/" + gateway.getId() + "/sync")
                        .with(dispatcherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchSize").value(2))
                .andExpect(jsonPath("$.data.acceptedCount").value(2))
                .andExpect(jsonPath("$.data.deduplicatedCount").value(0))
                .andExpect(jsonPath("$.data.syncMode").value("OFFLINE_REPLAY"));

        // Replay of same records (idempotency deduplication)
        mockMvc.perform(post("/api/v1/gateways/" + gateway.getId() + "/sync")
                        .with(dispatcherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchSize").value(2))
                .andExpect(jsonPath("$.data.acceptedCount").value(0))
                .andExpect(jsonPath("$.data.deduplicatedCount").value(2));
    }

    @Test
    void otherCompany_cannotAccessGateway() throws Exception {
        mockMvc.perform(get("/api/v1/gateways/" + gateway.getId()).with(otherCompanyAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/gateways"))
                .andExpect(status().isUnauthorized());
    }
}
