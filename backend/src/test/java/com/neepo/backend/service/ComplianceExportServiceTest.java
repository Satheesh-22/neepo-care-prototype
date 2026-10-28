package com.neepo.backend.service;

import com.neepo.backend.domain.CargoType;
import com.neepo.backend.domain.ComplianceExportFormat;
import com.neepo.backend.domain.ComplianceExportType;
import com.neepo.backend.domain.NetworkState;
import com.neepo.backend.domain.ObservationStatus;
import com.neepo.backend.domain.TelemetrySource;
import com.neepo.backend.dto.ComplianceExportManifestResponse;
import com.neepo.backend.dto.ComplianceExportPackageResponse;
import com.neepo.backend.dto.ComplianceVerificationResponse;
import com.neepo.backend.dto.TelemetryHistoryItemResponse;
import com.neepo.backend.entity.Cargo;
import com.neepo.backend.entity.ComplianceExportManifest;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.entity.Vehicle;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.AuditJdbcRepository;
import com.neepo.backend.repository.CargoRepository;
import com.neepo.backend.repository.ComplianceExportManifestRepository;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.backend.repository.TelemetryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.backend.repository.VehicleRepository;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceExportServiceTest {

    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("10000000-0000-4000-9000-000000000001");
    private static final UUID CARGO_ID = UUID.fromString("10000000-0000-4000-c000-000000000001");
    private static final UUID VEHICLE_ID = UUID.fromString("10000000-0000-4000-b000-000000000001");
    private static final UUID DRIVER_ID = UUID.fromString("10000000-0000-4000-d000-000000000001");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-4000-e000-000000000001");

    @Mock
    private TripRepository tripRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private TelemetryJdbcRepository telemetryRepository;
    @Mock
    private RecoveryQueryJdbcRepository recoveryQueryRepository;
    @Mock
    private AuditJdbcRepository auditRepository;
    @Mock
    private ComplianceExportManifestRepository manifestRepository;
    @Mock
    private AuditService auditService;

    private ComplianceExportService exportService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        exportService = new ComplianceExportService(
                tripRepository,
                cargoRepository,
                vehicleRepository,
                driverRepository,
                telemetryRepository,
                recoveryQueryRepository,
                auditRepository,
                manifestRepository,
                auditService,
                objectMapper
        );
    }

    @Test
    void generatesJsonLdCompliancePackageWithExcursionsAndDigest() {
        Trip trip = Trip.create(COMPANY_ID, VEHICLE_ID, DRIVER_ID, CARGO_ID, UUID.randomUUID(),
                "TRIP-COLD-01", Instant.now().plusSeconds(3600), 60, 30);
        when(tripRepository.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));

        Cargo cargo = Cargo.create(COMPANY_ID, "CARGO-VACCINE", CargoType.REFRIGERATED,
                "Pharmaceutical Vaccines", BigDecimal.valueOf(1500),
                BigDecimal.valueOf(2.0), BigDecimal.valueOf(8.0), false);
        when(cargoRepository.findByIdAndCompanyId(CARGO_ID, COMPANY_ID)).thenReturn(Optional.of(cargo));

        // Observation violating max temperature (10.5 > 8.0)
        TelemetryHistoryItemResponse tempExcursion = new TelemetryHistoryItemResponse(
                UUID.randomUUID(), Instant.now(), BigDecimal.valueOf(52.5), BigDecimal.valueOf(13.4),
                BigDecimal.valueOf(85.0), BigDecimal.valueOf(10.5), BigDecimal.valueOf(60.0),
                BigDecimal.valueOf(0.95), ObservationStatus.FRESH, TelemetrySource.VEHICLE_DEVICE, NetworkState.ONLINE
        );
        when(telemetryRepository.findHistory(eq(TRIP_ID), any(), any(), eq(2000)))
                .thenReturn(List.of(tempExcursion));

        PlanEvaluation eval = new PlanEvaluation(
                UUID.randomUUID(), true, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.1),
                BigDecimal.valueOf(0.9), BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(0.7), 0.95, Collections.emptyMap(),
                List.of("COLD_CHAIN_MAINTAINED"), Instant.now(), "1.0",
                "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890"
        );
        RecoveryPlan plan = new RecoveryPlan(
                UUID.randomUUID(), TRIP_ID, UUID.randomUUID(), RecoveryPlanType.WORKSHOP_REPAIR,
                RecoveryPlanStatus.SELECTED, UUID.randomUUID(), null, Collections.emptyList(),
                eval, Instant.now(), Instant.now()
        );
        when(recoveryQueryRepository.findPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of(plan));

        when(manifestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ComplianceExportPackageResponse result = exportService.generateTripExport(
                COMPANY_ID, TRIP_ID, ComplianceExportType.FULL_TRIP_DOSSIER, ComplianceExportFormat.JSON_LD, USER_ID, UUID.randomUUID()
        );

        assertThat(result).isNotNull();
        assertThat(result.mediaType()).isEqualTo("application/ld+json");
        assertThat(result.filename()).contains("trip-cold-01");
        assertThat(result.manifest()).isNotNull();
        assertThat(result.manifest().sha256Digest()).hasSize(64);
        assertThat(result.rawPayload()).contains("ComplianceAuditPackage");
        assertThat(result.rawPayload()).contains("OVER_TEMPERATURE");
        assertThat(result.rawPayload()).contains("a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890");

        verify(manifestRepository).save(any(ComplianceExportManifest.class));
        verify(auditService).record(any(), eq(USER_ID), eq("COMPLIANCE_EXPORT_GENERATED"), eq("COMPLIANCE_MANIFEST"), any(), any());
    }

    @Test
    void generatesCsvAndHtmlPackagesSuccessfully() {
        Trip trip = Trip.create(COMPANY_ID, VEHICLE_ID, DRIVER_ID, CARGO_ID, UUID.randomUUID(),
                "TRIP-CSV-01", Instant.now().plusSeconds(3600), 60, 30);
        when(tripRepository.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));
        when(manifestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ComplianceExportPackageResponse csvResult = exportService.generateTripExport(
                COMPANY_ID, TRIP_ID, ComplianceExportType.COLD_CHAIN_EXCURSION, ComplianceExportFormat.CSV, USER_ID, UUID.randomUUID()
        );
        assertThat(csvResult.mediaType()).isEqualTo("text/csv");
        assertThat(csvResult.rawPayload()).contains("RECORD_TYPE,TIMESTAMP");

        ComplianceExportPackageResponse htmlResult = exportService.generateTripExport(
                COMPANY_ID, TRIP_ID, ComplianceExportType.FULL_TRIP_DOSSIER, ComplianceExportFormat.PRINTABLE_HTML, USER_ID, UUID.randomUUID()
        );
        assertThat(htmlResult.mediaType()).isEqualTo("text/html");
        assertThat(htmlResult.rawPayload()).contains("<!DOCTYPE html>");
        assertThat(htmlResult.rawPayload()).contains("NEEPO-CARE Regulatory Audit Dossier");
    }

    @Test
    void verifiesPayloadIntegrityAndDetectsTampering() {
        String testPayload = "{\"test\": \"immutable payload\"}";
        String validDigest = ComplianceExportService.computeSha256(testPayload);

        ComplianceExportManifest manifest = ComplianceExportManifest.create(
                COMPANY_ID, TRIP_ID, ComplianceExportType.FULL_TRIP_DOSSIER, ComplianceExportFormat.JSON_LD,
                validDigest, 10, USER_ID, "{}"
        );
        UUID manifestId = UUID.randomUUID();
        when(manifestRepository.findByIdAndCompanyId(manifestId, COMPANY_ID)).thenReturn(Optional.of(manifest));

        // 1. Valid payload verification
        ComplianceVerificationResponse validResponse = exportService.verifyPayload(COMPANY_ID, manifestId, testPayload);
        assertThat(validResponse.valid()).isTrue();
        assertThat(validResponse.expectedDigest()).isEqualTo(validDigest);
        assertThat(validResponse.computedDigest()).isEqualTo(validDigest);

        // 2. Tampered payload verification
        String tamperedPayload = "{\"test\": \"tampered payload\"}";
        ComplianceVerificationResponse tamperedResponse = exportService.verifyPayload(COMPANY_ID, manifestId, tamperedPayload);
        assertThat(tamperedResponse.valid()).isFalse();
        assertThat(tamperedResponse.expectedDigest()).isEqualTo(validDigest);
        assertThat(tamperedResponse.computedDigest()).isNotEqualTo(validDigest);
        assertThat(tamperedResponse.message()).contains("Integrity violation");
    }

    @Test
    void throwsNotFoundForUnknownTrip() {
        when(tripRepository.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportService.generateTripExport(
                COMPANY_ID, TRIP_ID, ComplianceExportType.FULL_TRIP_DOSSIER, ComplianceExportFormat.JSON_LD, USER_ID, UUID.randomUUID()
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trip was not found");
    }
}
