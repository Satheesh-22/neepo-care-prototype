package com.neepo.backend.controller;

import com.neepo.backend.domain.CargoType;
import com.neepo.backend.domain.ComplianceExportFormat;
import com.neepo.backend.domain.ComplianceExportType;
import com.neepo.backend.entity.Cargo;
import com.neepo.backend.entity.ComplianceExportManifest;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.repository.CargoRepository;
import com.neepo.backend.repository.ComplianceExportManifestRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.backend.service.ComplianceExportService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.neepo.backend.repository.TelemetryJdbcRepository;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ComplianceExportControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @MockitoBean
    private TelemetryJdbcRepository telemetryRepository;

    @MockitoBean
    private RecoveryQueryJdbcRepository recoveryQueryRepository;

    @Autowired
    private ComplianceExportManifestRepository manifestRepository;

    private Trip testTrip;

    @BeforeEach
    void setUp() {
        manifestRepository.deleteAll();

        Cargo cargo = Cargo.create(COMPANY_ID, "CARGO-EXP-" + UUID.randomUUID().toString().substring(0, 8),
                CargoType.REFRIGERATED, "Biologics", BigDecimal.valueOf(500),
                BigDecimal.valueOf(2.0), BigDecimal.valueOf(8.0), false);
        cargo = cargoRepository.save(cargo);

        testTrip = Trip.create(COMPANY_ID, UUID.randomUUID(), UUID.randomUUID(), cargo.getId(),
                UUID.randomUUID(), "TRIP-EXP-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now().plusSeconds(3600), 60, 30);
        testTrip = tripRepository.save(testTrip);
    }

    @Test
    void generatesTripComplianceExportSuccessfullyWithDigestHeader() throws Exception {
        mockMvc.perform(post("/api/v1/compliance/exports/trip/" + testTrip.getId())
                        .with(dispatcher(COMPANY_ID))
                        .param("type", "FULL_TRIP_DOSSIER")
                        .param("format", "JSON_LD"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Neepo-Digest"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.manifest.exportType").value("FULL_TRIP_DOSSIER"))
                .andExpect(jsonPath("$.data.manifest.format").value("JSON_LD"))
                .andExpect(jsonPath("$.data.mediaType").value("application/ld+json"))
                .andExpect(jsonPath("$.data.rawPayload").isNotEmpty());
    }

    @Test
    void rejectsDriverRoleFromGeneratingComplianceExports() throws Exception {
        mockMvc.perform(post("/api/v1/compliance/exports/trip/" + testTrip.getId())
                        .with(driver(COMPANY_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsManifestsAndVerifiesDigestIntegrity() throws Exception {
        String payload = "{\"test\": \"payload for digest verification\"}";
        String digest = ComplianceExportService.computeSha256(payload);

        ComplianceExportManifest manifest = ComplianceExportManifest.create(
                COMPANY_ID, testTrip.getId(), ComplianceExportType.COLD_CHAIN_EXCURSION,
                ComplianceExportFormat.JSON_LD, digest, 5, UUID.randomUUID(), "{}"
        );
        manifest = manifestRepository.save(manifest);

        // 1. List manifests
        mockMvc.perform(get("/api/v1/compliance/exports")
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(manifest.getId().toString()))
                .andExpect(jsonPath("$.data[0].sha256Digest").value(digest));

        // 2. Tenant isolation - other company gets empty list
        mockMvc.perform(get("/api/v1/compliance/exports")
                        .with(dispatcher(OTHER_COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // 3. Verify valid payload
        mockMvc.perform(post("/api/v1/compliance/exports/" + manifest.getId() + "/verify")
                        .with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\": \"" + payload.replace("\"", "\\\"") + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.expectedDigest").value(digest))
                .andExpect(jsonPath("$.data.computedDigest").value(digest));

        // 4. Verify tampered payload
        mockMvc.perform(post("/api/v1/compliance/exports/" + manifest.getId() + "/verify")
                        .with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\": \"tampered content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));
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
