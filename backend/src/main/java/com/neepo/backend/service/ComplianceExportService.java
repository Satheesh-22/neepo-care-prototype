package com.neepo.backend.service;

import com.neepo.backend.domain.ComplianceExportFormat;
import com.neepo.backend.domain.ComplianceExportType;
import com.neepo.backend.dto.AuditEventResponse;
import com.neepo.backend.dto.ComplianceExportManifestResponse;
import com.neepo.backend.dto.ComplianceExportPackageResponse;
import com.neepo.backend.dto.ComplianceVerificationResponse;
import com.neepo.backend.dto.TelemetryHistoryItemResponse;
import com.neepo.backend.entity.Cargo;
import com.neepo.backend.entity.ComplianceExportManifest;
import com.neepo.backend.entity.Driver;
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
import com.neepo.contracts.RecoveryPlan;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ComplianceExportService {

    private final TripRepository tripRepository;
    private final CargoRepository cargoRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TelemetryJdbcRepository telemetryRepository;
    private final RecoveryQueryJdbcRepository recoveryQueryRepository;
    private final AuditJdbcRepository auditRepository;
    private final ComplianceExportManifestRepository manifestRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ComplianceExportService(
            TripRepository tripRepository,
            CargoRepository cargoRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            TelemetryJdbcRepository telemetryRepository,
            RecoveryQueryJdbcRepository recoveryQueryRepository,
            AuditJdbcRepository auditRepository,
            ComplianceExportManifestRepository manifestRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.tripRepository = tripRepository;
        this.cargoRepository = cargoRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.telemetryRepository = telemetryRepository;
        this.recoveryQueryRepository = recoveryQueryRepository;
        this.auditRepository = auditRepository;
        this.manifestRepository = manifestRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ComplianceExportPackageResponse generateTripExport(
            UUID companyId,
            UUID tripId,
            ComplianceExportType exportType,
            ComplianceExportFormat format,
            UUID userId,
            UUID traceId
    ) {
        Objects.requireNonNull(companyId, "Company ID is required");
        Objects.requireNonNull(tripId, "Trip ID is required");
        ComplianceExportType effectiveType = exportType != null ? exportType : ComplianceExportType.FULL_TRIP_DOSSIER;
        ComplianceExportFormat effectiveFormat = format != null ? format : ComplianceExportFormat.JSON_LD;

        Trip trip = tripRepository.findByIdAndCompanyId(tripId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found: " + tripId));

        Optional<Cargo> cargoOpt = cargoRepository.findByIdAndCompanyId(trip.getCargoId(), companyId);
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByIdAndCompanyId(trip.getVehicleId(), companyId);
        Optional<Driver> driverOpt = driverRepository.findByIdAndCompanyId(trip.getDriverId(), companyId);

        List<TelemetryHistoryItemResponse> telemetry = telemetryRepository.findHistory(tripId, null, null, 2000);
        List<RecoveryPlan> recoveryPlans = recoveryQueryRepository.findPlans(companyId, tripId);
        List<AuditEventResponse> tripAudits = auditRepository.listByAggregate("TRIP", tripId, 50);

        List<Map<String, Object>> excursions = findExcursions(telemetry, cargoOpt.orElse(null));

        int recordCount = telemetry.size() + recoveryPlans.size() + tripAudits.size();

        String rawPayload;
        String mediaType;
        String fileExt;

        switch (effectiveFormat) {
            case CSV -> {
                rawPayload = buildCsvPayload(trip, cargoOpt.orElse(null), vehicleOpt.orElse(null), driverOpt.orElse(null), excursions, recoveryPlans, tripAudits, effectiveType);
                mediaType = "text/csv";
                fileExt = "csv";
            }
            case PRINTABLE_HTML -> {
                rawPayload = buildHtmlPayload(trip, cargoOpt.orElse(null), vehicleOpt.orElse(null), driverOpt.orElse(null), excursions, recoveryPlans, tripAudits, effectiveType);
                mediaType = "text/html";
                fileExt = "html";
            }
            case JSON_LD -> {
                rawPayload = buildJsonLdPayload(trip, cargoOpt.orElse(null), vehicleOpt.orElse(null), driverOpt.orElse(null), excursions, recoveryPlans, tripAudits, effectiveType);
                mediaType = "application/ld+json";
                fileExt = "jsonld";
            }
            default -> throw new IllegalArgumentException("Unsupported format: " + effectiveFormat);
        }

        String sha256Digest = computeSha256(rawPayload);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("regulatoryStandard", "FDA 21 CFR Part 11 / GDP Guidelines");
        metadata.put("tripReference", trip.getReferenceCode());
        metadata.put("excursionCount", excursions.size());
        metadata.put("carePlanCount", recoveryPlans.size());
        metadata.put("telemetryObservationCount", telemetry.size());

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            metadataJson = "{}";
        }

        ComplianceExportManifest manifest = ComplianceExportManifest.create(
                companyId,
                tripId,
                effectiveType,
                effectiveFormat,
                sha256Digest,
                recordCount,
                userId,
                metadataJson
        );
        ComplianceExportManifest savedManifest = manifestRepository.save(manifest);

        if (auditService != null) {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("manifestId", savedManifest.getId());
            auditDetails.put("tripId", tripId);
            auditDetails.put("exportType", effectiveType.name());
            auditDetails.put("format", effectiveFormat.name());
            auditDetails.put("sha256Digest", sha256Digest);
            auditDetails.put("recordCount", recordCount);
            auditService.record(traceId, userId, "COMPLIANCE_EXPORT_GENERATED", "COMPLIANCE_MANIFEST", savedManifest.getId(), auditDetails);
        }

        String filename = String.format("neepo-compliance-%s-%s.%s",
                trip.getReferenceCode().toLowerCase().replaceAll("[^a-z0-9_-]", "_"),
                effectiveType.name().toLowerCase(),
                fileExt);

        return new ComplianceExportPackageResponse(
                ComplianceExportManifestResponse.from(savedManifest),
                rawPayload,
                mediaType,
                filename
        );
    }

    public List<ComplianceExportManifestResponse> listManifests(UUID companyId, int limit) {
        Objects.requireNonNull(companyId, "Company ID is required");
        int effectiveLimit = Math.min(Math.max(limit, 1), 200);
        return manifestRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, PageRequest.of(0, effectiveLimit))
                .stream()
                .map(ComplianceExportManifestResponse::from)
                .toList();
    }

    public ComplianceExportManifestResponse getManifest(UUID companyId, UUID manifestId) {
        Objects.requireNonNull(companyId, "Company ID is required");
        Objects.requireNonNull(manifestId, "Manifest ID is required");
        return manifestRepository.findByIdAndCompanyId(manifestId, companyId)
                .map(ComplianceExportManifestResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("COMPLIANCE_MANIFEST_NOT_FOUND", "Manifest not found: " + manifestId));
    }

    public ComplianceVerificationResponse verifyPayload(UUID companyId, UUID manifestId, String rawPayload) {
        Objects.requireNonNull(companyId, "Company ID is required");
        Objects.requireNonNull(manifestId, "Manifest ID is required");
        Objects.requireNonNull(rawPayload, "Raw payload is required for verification");

        ComplianceExportManifest manifest = manifestRepository.findByIdAndCompanyId(manifestId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("COMPLIANCE_MANIFEST_NOT_FOUND", "Manifest not found: " + manifestId));

        String computed = computeSha256(rawPayload);
        boolean valid = computed.equalsIgnoreCase(manifest.getSha256Digest());
        String message = valid
                ? "Cryptographic integrity verified: SHA-256 digest matches registered compliance manifest"
                : "Integrity violation: computed SHA-256 digest does NOT match manifest (data altered or corrupted)";

        return new ComplianceVerificationResponse(
                manifestId,
                valid,
                manifest.getSha256Digest(),
                computed,
                Instant.now(),
                message
        );
    }

    private List<Map<String, Object>> findExcursions(List<TelemetryHistoryItemResponse> telemetry, Cargo cargo) {
        List<Map<String, Object>> excursions = new ArrayList<>();
        if (cargo == null || (cargo.getMinimumTemperatureCelsius() == null && cargo.getMaximumTemperatureCelsius() == null)) {
            return excursions;
        }

        BigDecimal minTemp = cargo.getMinimumTemperatureCelsius();
        BigDecimal maxTemp = cargo.getMaximumTemperatureCelsius();

        for (TelemetryHistoryItemResponse point : telemetry) {
            BigDecimal cargoTemp = point.cargoTemperatureCelsius();
            if (cargoTemp == null) {
                continue;
            }

            boolean belowMin = minTemp != null && cargoTemp.compareTo(minTemp) < 0;
            boolean aboveMax = maxTemp != null && cargoTemp.compareTo(maxTemp) > 0;

            if (belowMin || aboveMax) {
                BigDecimal limit = belowMin ? minTemp : maxTemp;
                BigDecimal deviation = cargoTemp.subtract(limit).abs();

                String severity = "MEDIUM";
                if (deviation.compareTo(BigDecimal.valueOf(3.0)) > 0) {
                    severity = "CRITICAL";
                } else if (deviation.compareTo(BigDecimal.valueOf(1.0)) > 0) {
                    severity = "HIGH";
                }

                Map<String, Object> excursion = new LinkedHashMap<>();
                excursion.put("eventId", point.eventId());
                excursion.put("recordedAt", point.recordedAt().toString());
                excursion.put("cargoTemperatureCelsius", cargoTemp);
                excursion.put("permittedMinCelsius", minTemp);
                excursion.put("permittedMaxCelsius", maxTemp);
                excursion.put("deviationCelsius", deviation);
                excursion.put("violationType", belowMin ? "UNDER_TEMPERATURE" : "OVER_TEMPERATURE");
                excursion.put("severity", severity);
                excursion.put("engineTemperatureCelsius", point.engineTemperatureCelsius());
                excursion.put("speedKph", point.speedKph());
                excursion.put("latitude", point.latitude());
                excursion.put("longitude", point.longitude());
                excursions.add(excursion);
            }
        }
        return excursions;
    }

    private String buildJsonLdPayload(
            Trip trip,
            Cargo cargo,
            Vehicle vehicle,
            Driver driver,
            List<Map<String, Object>> excursions,
            List<RecoveryPlan> recoveryPlans,
            List<AuditEventResponse> tripAudits,
            ComplianceExportType exportType
    ) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", List.of(
                "https://www.w3.org/ns/activitystreams",
                "https://w3id.org/neepo/compliance/v1"
        ));
        root.put("@type", "ComplianceAuditPackage");
        root.put("regulatoryStandard", "FDA 21 CFR Part 11 / GDP Guidelines (EU Good Distribution Practice)");
        root.put("exportType", exportType.name());
        root.put("generatedAt", Instant.now().toString());

        Map<String, Object> tripMeta = new LinkedHashMap<>();
        tripMeta.put("tripId", trip.getId());
        tripMeta.put("referenceCode", trip.getReferenceCode());
        tripMeta.put("status", trip.getStatus().name());
        tripMeta.put("deliveryDeadline", trip.getDeliveryDeadline().toString());
        tripMeta.put("startedAt", trip.getStartedAt() != null ? trip.getStartedAt().toString() : null);
        tripMeta.put("completedAt", trip.getCompletedAt() != null ? trip.getCompletedAt().toString() : null);
        root.put("trip", tripMeta);

        if (cargo != null) {
            Map<String, Object> cargoMeta = new LinkedHashMap<>();
            cargoMeta.put("referenceCode", cargo.getReferenceCode());
            cargoMeta.put("cargoType", cargo.getCargoType().name());
            cargoMeta.put("description", cargo.getDescription());
            cargoMeta.put("weightKg", cargo.getWeightKg());
            cargoMeta.put("minimumTemperatureCelsius", cargo.getMinimumTemperatureCelsius());
            cargoMeta.put("maximumTemperatureCelsius", cargo.getMaximumTemperatureCelsius());
            cargoMeta.put("hazardous", cargo.isHazardous());
            root.put("cargo", cargoMeta);
        }

        if (vehicle != null) {
            Map<String, Object> vehicleMeta = new LinkedHashMap<>();
            vehicleMeta.put("registrationNumber", vehicle.getRegistrationNumber());
            vehicleMeta.put("vehicleType", vehicle.getVehicleType());
            vehicleMeta.put("refrigerationSupported", vehicle.isRefrigerationSupported());
            root.put("vehicle", vehicleMeta);
        }

        if (driver != null) {
            Map<String, Object> driverMeta = new LinkedHashMap<>();
            driverMeta.put("licenseNumber", driver.getLicenseNumber());
            driverMeta.put("driverStatus", driver.getStatus().name());
            root.put("driver", driverMeta);
        }

        if (exportType == ComplianceExportType.COLD_CHAIN_EXCURSION || exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            Map<String, Object> excursionSummary = new LinkedHashMap<>();
            excursionSummary.put("excursionCount", excursions.size());
            excursionSummary.put("compliant", excursions.isEmpty());
            excursionSummary.put("records", excursions);
            root.put("coldChainAudit", excursionSummary);
        }

        if (exportType == ComplianceExportType.CARE_DECISION_CHAIN || exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            List<Map<String, Object>> plansData = new ArrayList<>();
            for (RecoveryPlan plan : recoveryPlans) {
                Map<String, Object> planMap = new LinkedHashMap<>();
                planMap.put("planId", plan.id());
                planMap.put("type", plan.type().name());
                planMap.put("status", plan.status().name());
                planMap.put("providerId", plan.providerId());
                planMap.put("generatedAt", plan.generatedAt().toString());
                if (plan.evaluation() != null) {
                    Map<String, Object> eval = new LinkedHashMap<>();
                    eval.put("feasible", plan.evaluation().feasible());
                    eval.put("finalUtility", plan.evaluation().finalUtility());
                    eval.put("riskScore", plan.evaluation().riskScore());
                    eval.put("benefitScore", plan.evaluation().benefitScore());
                    eval.put("inputHash", plan.evaluation().inputHash());
                    eval.put("explanationCodes", plan.evaluation().explanationCodes());
                    eval.put("freshnessSummary", plan.evaluation().freshnessSummary());
                    planMap.put("evaluation", eval);
                }
                plansData.add(planMap);
            }
            root.put("careDecisionChain", plansData);
        }

        if (exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            root.put("auditEvents", tripAudits);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize compliance JSON-LD", e);
        }
    }

    private String buildCsvPayload(
            Trip trip,
            Cargo cargo,
            Vehicle vehicle,
            Driver driver,
            List<Map<String, Object>> excursions,
            List<RecoveryPlan> recoveryPlans,
            List<AuditEventResponse> tripAudits,
            ComplianceExportType exportType
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# NEEPO-CARE Cryptographic Compliance Export (FDA 21 CFR Part 11 / GDP)\n");
        sb.append(String.format("# Trip: %s | Company ID: %s | Export: %s\n", trip.getReferenceCode(), trip.getCompanyId(), exportType.name()));
        sb.append(String.format("# Generated At: %s | Permitted Temp Range: [%s, %s] C\n",
                Instant.now(),
                cargo != null ? cargo.getMinimumTemperatureCelsius() : "N/A",
                cargo != null ? cargo.getMaximumTemperatureCelsius() : "N/A"));
        sb.append("# ------------------------------------------------------------------------------------\n");
        sb.append("RECORD_TYPE,TIMESTAMP,IDENTIFIER,STATUS_OR_SEVERITY,VALUE_PRIMARY,VALUE_SECONDARY,DEVIATION,DETAILS,INPUT_HASH\n");

        if (exportType == ComplianceExportType.COLD_CHAIN_EXCURSION || exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            for (Map<String, Object> ex : excursions) {
                sb.append(String.format("EXCURSION,%s,%s,%s,%s,%s,%s,\"%s\",N/A\n",
                        ex.get("recordedAt"),
                        ex.get("eventId"),
                        ex.get("severity"),
                        ex.get("cargoTemperatureCelsius"),
                        ex.get("engineTemperatureCelsius"),
                        ex.get("deviationCelsius"),
                        ex.get("violationType")
                ));
            }
        }

        if (exportType == ComplianceExportType.CARE_DECISION_CHAIN || exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            for (RecoveryPlan plan : recoveryPlans) {
                String inputHash = plan.evaluation() != null ? plan.evaluation().inputHash() : "N/A";
                BigDecimal utility = plan.evaluation() != null ? plan.evaluation().finalUtility() : BigDecimal.ZERO;
                sb.append(String.format("CARE_PLAN,%s,%s,%s,%s,%s,0.00,\"PlanType=%s; Feasible=%s\",%s\n",
                        plan.generatedAt(),
                        plan.id(),
                        plan.status().name(),
                        utility != null ? utility.toString() : "0.00",
                        plan.providerId() != null ? plan.providerId() : "NONE",
                        plan.type().name(),
                        plan.evaluation() != null && plan.evaluation().feasible(),
                        inputHash
                ));
            }
        }

        if (exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            for (AuditEventResponse audit : tripAudits) {
                sb.append(String.format("AUDIT_EVENT,%s,%s,%s,N/A,N/A,0.00,\"Actor=%s; EventType=%s\",N/A\n",
                        audit.occurredAt(),
                        audit.id(),
                        audit.eventType(),
                        audit.actorUserId() != null ? audit.actorUserId() : "SYSTEM",
                        audit.eventType()
                ));
            }
        }

        return sb.toString();
    }

    private String buildHtmlPayload(
            Trip trip,
            Cargo cargo,
            Vehicle vehicle,
            Driver driver,
            List<Map<String, Object>> excursions,
            List<RecoveryPlan> recoveryPlans,
            List<AuditEventResponse> tripAudits,
            ComplianceExportType exportType
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<title>NEEPO-CARE Regulatory Audit Dossier - ").append(trip.getReferenceCode()).append("</title>\n");
        sb.append("<style>\n");
        sb.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; line-height: 1.5; margin: 30px auto; max-width: 900px; padding: 20px; }\n");
        sb.append(".header { border-bottom: 2px solid #0284c7; padding-bottom: 16px; margin-bottom: 24px; }\n");
        sb.append(".title { font-size: 24px; font-weight: 700; color: #0f172a; margin: 0; }\n");
        sb.append(".subtitle { font-size: 14px; color: #64748b; margin-top: 4px; }\n");
        sb.append(".badge { display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; }\n");
        sb.append(".badge-success { background: #dcfce7; color: #166534; }\n");
        sb.append(".badge-danger { background: #fee2e2; color: #991b1b; }\n");
        sb.append(".badge-warning { background: #fef3c7; color: #92400e; }\n");
        sb.append(".badge-info { background: #e0f2fe; color: #0369a1; }\n");
        sb.append(".grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 24px; }\n");
        sb.append(".card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 14px; }\n");
        sb.append(".card-title { font-size: 12px; font-weight: 600; text-transform: uppercase; color: #64748b; margin-bottom: 6px; }\n");
        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 12px; margin-bottom: 24px; font-size: 13px; }\n");
        sb.append("th { background: #f1f5f9; text-align: left; padding: 8px 10px; border-bottom: 1px solid #cbd5e1; font-weight: 600; color: #334155; }\n");
        sb.append("td { padding: 8px 10px; border-bottom: 1px solid #e2e8f0; }\n");
        sb.append(".hash { font-family: monospace; font-size: 11px; word-break: break-all; color: #0f172a; background: #f1f5f9; padding: 2px 4px; border-radius: 3px; }\n");
        sb.append(".seal-box { background: #f0fdf4; border: 1px solid #86efac; border-radius: 6px; padding: 16px; text-align: center; margin-top: 30px; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<div class=\"header\">\n");
        sb.append("  <div class=\"badge badge-info\">FDA 21 CFR Part 11 / EU GDP Compliance Certificate</div>\n");
        sb.append("  <h1 class=\"title\">NEEPO-CARE Regulatory Audit Dossier</h1>\n");
        sb.append("  <div class=\"subtitle\">Trip Reference: <strong>").append(trip.getReferenceCode()).append("</strong> | Generated At: ").append(Instant.now().toString()).append("</div>\n");
        sb.append("</div>\n");

        sb.append("<div class=\"grid\">\n");
        sb.append("  <div class=\"card\">\n");
        sb.append("    <div class=\"card-title\">Cargo & Cold-Chain Parameters</div>\n");
        if (cargo != null) {
            sb.append("    <div><strong>Ref:</strong> ").append(cargo.getReferenceCode()).append(" (").append(cargo.getCargoType()).append(")</div>\n");
            sb.append("    <div><strong>Description:</strong> ").append(cargo.getDescription()).append("</div>\n");
            sb.append("    <div><strong>Required Temp:</strong> ").append(cargo.getMinimumTemperatureCelsius()).append(" &deg;C to ").append(cargo.getMaximumTemperatureCelsius()).append(" &deg;C</div>\n");
        } else {
            sb.append("    <div>Standard Cargo (Ambient)</div>\n");
        }
        sb.append("  </div>\n");

        sb.append("  <div class=\"card\">\n");
        sb.append("    <div class=\"card-title\">Regulatory Compliance Assessment</div>\n");
        if (excursions.isEmpty()) {
            sb.append("    <div><span class=\"badge badge-success\">COMPLIANT</span> Zero cold-chain excursions recorded</div>\n");
        } else {
            sb.append("    <div><span class=\"badge badge-danger\">EXCURSIONS DETECTED</span> ").append(excursions.size()).append(" excursion event(s) recorded</div>\n");
        }
        sb.append("    <div style=\"margin-top:6px;\"><strong>CARE Re-planning Executions:</strong> ").append(recoveryPlans.size()).append(" candidate plan(s) evaluated</div>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        if (exportType == ComplianceExportType.COLD_CHAIN_EXCURSION || exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            sb.append("<h2>Cold-Chain Telemetry & Temperature Excursion Log</h2>\n");
            if (excursions.isEmpty()) {
                sb.append("<p>No thermal violations detected. All recorded cargo temperatures maintained within approved regulatory limits.</p>\n");
            } else {
                sb.append("<table>\n");
                sb.append("  <thead><tr><th>Timestamp</th><th>Recorded Temp</th><th>Deviation</th><th>Type</th><th>Severity</th><th>Engine Temp</th><th>Speed</th></tr></thead>\n");
                sb.append("  <tbody>\n");
                for (Map<String, Object> ex : excursions) {
                    sb.append("    <tr>\n");
                    sb.append("      <td>").append(ex.get("recordedAt")).append("</td>\n");
                    sb.append("      <td><strong>").append(ex.get("cargoTemperatureCelsius")).append(" &deg;C</strong></td>\n");
                    sb.append("      <td>+").append(ex.get("deviationCelsius")).append(" &deg;C</td>\n");
                    sb.append("      <td>").append(ex.get("violationType")).append("</td>\n");
                    sb.append("      <td><span class=\"badge badge-danger\">").append(ex.get("severity")).append("</span></td>\n");
                    sb.append("      <td>").append(ex.get("engineTemperatureCelsius")).append(" &deg;C</td>\n");
                    sb.append("      <td>").append(ex.get("speedKph")).append(" km/h</td>\n");
                    sb.append("    </tr>\n");
                }
                sb.append("  </tbody>\n</table>\n");
            }
        }

        if (exportType == ComplianceExportType.CARE_DECISION_CHAIN || exportType == ComplianceExportType.FULL_TRIP_DOSSIER) {
            sb.append("<h2>CARE Deterministic Decision & Solver Proof Chain</h2>\n");
            sb.append("<table>\n");
            sb.append("  <thead><tr><th>Generated At</th><th>Plan Type</th><th>Status</th><th>Utility</th><th>Feasible</th><th>Solver Input SHA-256 Digest</th></tr></thead>\n");
            sb.append("  <tbody>\n");
            for (RecoveryPlan plan : recoveryPlans) {
                String inputHash = plan.evaluation() != null ? plan.evaluation().inputHash() : "N/A";
                BigDecimal utility = plan.evaluation() != null ? plan.evaluation().finalUtility() : BigDecimal.ZERO;
                boolean feasible = plan.evaluation() != null && plan.evaluation().feasible();
                sb.append("    <tr>\n");
                sb.append("      <td>").append(plan.generatedAt()).append("</td>\n");
                sb.append("      <td><strong>").append(plan.type()).append("</strong></td>\n");
                sb.append("      <td><span class=\"badge ").append(plan.status().name().equals("SELECTED") ? "badge-success" : "badge-info").append("\">").append(plan.status()).append("</span></td>\n");
                sb.append("      <td>").append(utility != null ? utility.toPlainString() : "0.00").append("</td>\n");
                sb.append("      <td>").append(feasible ? "<span class=\"badge badge-success\">YES</span>" : "<span class=\"badge badge-danger\">NO</span>").append("</td>\n");
                sb.append("      <td><span class=\"hash\">").append(inputHash).append("</span></td>\n");
                sb.append("    </tr>\n");
            }
            sb.append("  </tbody>\n</table>\n");
        }

        sb.append("<div class=\"seal-box\">\n");
        sb.append("  <div style=\"font-weight: 700; color: #166534; font-size: 15px;\">NEEPO-CARE Cryptographic Compliance Seal</div>\n");
        sb.append("  <div style=\"color: #15803d; font-size: 12px; margin-top: 4px;\">This document is cryptographically anchored. Tampering with any character invalidates the SHA-256 digest registered in the immutable audit manifest.</div>\n");
        sb.append("</div>\n");

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    public static String computeSha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }
}
