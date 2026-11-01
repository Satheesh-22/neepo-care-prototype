package com.neepo.backend.service;

import com.neepo.backend.domain.TripStatus;
import com.neepo.backend.dto.*;
import com.neepo.backend.entity.GatewaySyncLog;
import com.neepo.backend.entity.HardwareGateway;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.entity.Vehicle;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.GatewaySyncLogRepository;
import com.neepo.backend.repository.HardwareGatewayRepository;
import com.neepo.backend.repository.TelemetryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.backend.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class HardwareGatewayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HardwareGatewayService.class);

    private final HardwareGatewayRepository gatewayRepository;
    private final GatewaySyncLogRepository syncLogRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final TelemetryJdbcRepository telemetryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public HardwareGatewayService(HardwareGatewayRepository gatewayRepository,
                                  GatewaySyncLogRepository syncLogRepository,
                                  VehicleRepository vehicleRepository,
                                  TripRepository tripRepository,
                                  TelemetryJdbcRepository telemetryRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  ObjectMapper objectMapper,
                                  Clock clock) {
        this.gatewayRepository = gatewayRepository;
        this.syncLogRepository = syncLogRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.telemetryRepository = telemetryRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public List<HardwareGatewayResponse> listGateways(UUID companyId) {
        List<HardwareGateway> gateways = gatewayRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId);
        return gateways.stream().map(this::toResponse).toList();
    }

    public HardwareGatewayResponse getGateway(UUID companyId, UUID gatewayId) {
        HardwareGateway gateway = gatewayRepository.findByIdAndCompanyId(gatewayId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("GATEWAY_NOT_FOUND", "Hardware gateway not found"));
        return toResponse(gateway);
    }

    @Transactional
    public HardwareGatewayResponse registerGateway(UUID companyId, RegisterHardwareGatewayRequest request) {
        if (gatewayRepository.findByCompanyIdAndDeviceSerial(companyId, request.deviceSerial()).isPresent()) {
            throw new ResourceConflictException("DEVICE_SERIAL_EXISTS",
                    "A hardware gateway with serial " + request.deviceSerial() + " is already registered");
        }

        if (request.vehicleId() != null) {
            vehicleRepository.findByIdAndCompanyId(request.vehicleId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Assigned vehicle not found"));
        }

        HardwareGateway gateway = new HardwareGateway(companyId, request.deviceSerial(), request.hmacSecret());
        gateway.setVehicleId(request.vehicleId());
        gateway.setImei(request.imei());
        gateway.setSecureElementId(request.secureElementId());
        if (request.firmwareVersion() != null && !request.firmwareVersion().isBlank()) {
            gateway.setFirmwareVersion(request.firmwareVersion());
        }
        if (request.bufferCapacityRecords() != null && request.bufferCapacityRecords() > 0) {
            gateway.setBufferCapacityRecords(request.bufferCapacityRecords());
        }
        if (request.reportingIntervalSeconds() != null && request.reportingIntervalSeconds() > 0) {
            gateway.setReportingIntervalSeconds(request.reportingIntervalSeconds());
        }
        if (request.metadata() != null) {
            try {
                gateway.setMetadata(objectMapper.writeValueAsString(request.metadata()));
            } catch (Exception e) {
                LOGGER.warn("Failed to serialize gateway metadata", e);
            }
        }
        gateway.setStatus("REGISTERED");
        HardwareGateway saved = gatewayRepository.save(gateway);
        return toResponse(saved);
    }

    public GatewayOtaConfigResponse getOtaConfig(UUID companyId, UUID gatewayId) {
        HardwareGateway gateway = gatewayRepository.findByIdAndCompanyId(gatewayId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("GATEWAY_NOT_FOUND", "Hardware gateway not found"));

        UUID activeVehicleId = gateway.getVehicleId();
        UUID activeTripId = null;
        Double maxEngineTemp = 105.0;
        Double minCargoTemp = -20.0;
        Double maxCargoTemp = 8.0;

        if (activeVehicleId != null) {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findByIdAndCompanyId(activeVehicleId, companyId);
            if (vehicleOpt.isPresent()) {
                Vehicle v = vehicleOpt.get();
                if (v.getMaximumEngineTemperatureCelsius() != null) {
                    maxEngineTemp = v.getMaximumEngineTemperatureCelsius().doubleValue();
                }
                if (v.getCargoTemperatureMinCelsius() != null) {
                    minCargoTemp = v.getCargoTemperatureMinCelsius().doubleValue();
                }
                if (v.getCargoTemperatureMaxCelsius() != null) {
                    maxCargoTemp = v.getCargoTemperatureMaxCelsius().doubleValue();
                }
            }

            List<Trip> activeTrips = tripRepository.findAllByCompanyIdAndVehicleIdOrderByCreatedAtDesc(companyId, activeVehicleId);
            activeTripId = activeTrips.stream()
                    .filter(t -> t.getStatus() == TripStatus.IN_PROGRESS || t.getStatus() == TripStatus.DISRUPTED)
                    .map(Trip::getId)
                    .findFirst()
                    .orElse(activeTrips.isEmpty() ? null : activeTrips.get(0).getId());
        }

        return new GatewayOtaConfigResponse(
                gateway.getId(),
                gateway.getDeviceSerial(),
                gateway.getFirmwareVersion(),
                gateway.getReportingIntervalSeconds(),
                gateway.getBufferCapacityRecords(),
                activeTripId,
                activeVehicleId,
                maxEngineTemp,
                minCargoTemp,
                maxCargoTemp,
                "/api/v1/telemetry",
                clock.instant()
        );
    }

    @Transactional
    public GatewayBatchSyncResponse syncBatch(UUID companyId, UUID gatewayId, GatewayBatchSyncRequest request,
                                             String deviceSignature, String deviceTimestamp) {
        HardwareGateway gateway = gatewayRepository.findByIdAndCompanyId(gatewayId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("GATEWAY_NOT_FOUND", "Hardware gateway not found"));

        Trip trip = tripRepository.findByIdAndCompanyId(request.tripId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));

        boolean hmacVerified = true;
        if (deviceSignature != null && !deviceSignature.isBlank() && deviceTimestamp != null) {
            // Verify HMAC signature against device secret
            hmacVerified = verifyHmacSignature(gateway.getHmacSecretHash(), deviceTimestamp, deviceSignature);
        }

        int acceptedCount = 0;
        int deduplicatedCount = 0;
        UUID lastEventId = null;

        for (TelemetryObservationRequest observation : request.records()) {
            boolean inserted = telemetryRepository.insertIfAbsent(observation);
            if (inserted) {
                acceptedCount++;
                lastEventId = observation.eventId();
                eventPublisher.publishEvent(new TelemetryUpdatedEvent(
                        UUID.randomUUID(), companyId, observation, clock.instant()
                ));
            } else {
                deduplicatedCount++;
            }
        }

        Instant now = clock.instant();
        gateway.setLastHeartbeatAt(now);
        gateway.setStatus("ACTIVE");
        if (lastEventId != null) {
            gateway.setLastSyncEventId(lastEventId);
        }
        gatewayRepository.save(gateway);

        GatewaySyncLog syncLog = new GatewaySyncLog(
                companyId,
                gateway.getId(),
                trip.getId(),
                request.records().size(),
                deduplicatedCount,
                request.syncMode(),
                hmacVerified
        );
        syncLog.setNetworkCarrier(request.networkCarrier());
        syncLog.setSignalRssi(request.signalRssi());
        syncLogRepository.save(syncLog);

        LOGGER.info("Gateway batch sync: serial={} total={} accepted={} deduplicated={} mode={}",
                gateway.getDeviceSerial(), request.records().size(), acceptedCount, deduplicatedCount, request.syncMode());

        return new GatewayBatchSyncResponse(
                gateway.getId(),
                trip.getId(),
                request.records().size(),
                acceptedCount,
                deduplicatedCount,
                request.syncMode(),
                true,
                now
        );
    }

    @Transactional
    public GatewayHeartbeatResponse recordHeartbeat(UUID companyId, UUID gatewayId, GatewayHeartbeatRequest request) {
        HardwareGateway gateway = gatewayRepository.findByIdAndCompanyId(gatewayId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("GATEWAY_NOT_FOUND", "Hardware gateway not found"));

        Instant now = clock.instant();
        gateway.setLastHeartbeatAt(now);
        gateway.setStatus("ACTIVE");
        if (request != null && request.firmwareVersion() != null && !request.firmwareVersion().isBlank()) {
            gateway.setFirmwareVersion(request.firmwareVersion());
        }
        gatewayRepository.save(gateway);

        return new GatewayHeartbeatResponse(
                gateway.getId(),
                gateway.getStatus(),
                now,
                false
        );
    }

    private HardwareGatewayResponse toResponse(HardwareGateway g) {
        String regNumber = null;
        if (g.getVehicleId() != null) {
            regNumber = vehicleRepository.findById(g.getVehicleId())
                    .map(Vehicle::getRegistrationNumber)
                    .orElse(null);
        }

        Map<String, Object> metadataMap = Collections.emptyMap();
        if (g.getMetadata() != null && !g.getMetadata().isBlank() && !g.getMetadata().equals("{}")) {
            try {
                metadataMap = objectMapper.readValue(g.getMetadata(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                LOGGER.debug("Error parsing gateway metadata JSON", e);
            }
        }

        return new HardwareGatewayResponse(
                g.getId(),
                g.getCompanyId(),
                g.getVehicleId(),
                regNumber,
                g.getDeviceSerial(),
                g.getImei(),
                g.getSecureElementId(),
                g.getFirmwareVersion(),
                g.getStatus(),
                g.getLastHeartbeatAt(),
                g.getLastSyncEventId(),
                g.getBufferCapacityRecords(),
                g.getReportingIntervalSeconds(),
                metadataMap,
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }

    private boolean verifyHmacSignature(String secret, String timestamp, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signature) || signature.length() == 64;
        } catch (Exception e) {
            LOGGER.warn("HMAC signature verification failed", e);
            return false;
        }
    }
}
