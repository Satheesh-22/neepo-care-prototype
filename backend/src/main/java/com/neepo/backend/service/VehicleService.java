package com.neepo.backend.service;

import com.neepo.backend.domain.VehicleStatus;
import com.neepo.backend.dto.CreateVehicleDetailedRequest;
import com.neepo.backend.dto.CreateVehicleRequest;
import com.neepo.backend.dto.UpdateVehicleRequest;
import com.neepo.backend.dto.VehicleDetailedResponse;
import com.neepo.backend.dto.VehicleResponse;
import com.neepo.backend.entity.Driver;
import com.neepo.backend.entity.DriverVehicleAssignment;
import com.neepo.backend.entity.Vehicle;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.DriverVehicleAssignmentRepository;
import com.neepo.backend.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicles;
    private final DriverVehicleAssignmentRepository assignments;
    private final DriverRepository drivers;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired
    public VehicleService(VehicleRepository vehicles,
                          DriverVehicleAssignmentRepository assignments,
                          DriverRepository drivers,
                          AuditService auditService) {
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.drivers = drivers;
        this.auditService = auditService;
    }

    public VehicleService(VehicleRepository vehicles) {
        this(vehicles, null, null, null);
    }

    @Transactional
    public VehicleResponse create(UUID companyId, CreateVehicleRequest request) {
        String registration = request.registrationNumber().trim().toUpperCase(Locale.ROOT);
        if (vehicles.existsByCompanyIdAndRegistrationNumberIgnoreCase(companyId, registration)) {
            throw new ResourceConflictException("VEHICLE_REGISTRATION_EXISTS",
                    "A vehicle with this registration already exists");
        }
        Vehicle vehicle = Vehicle.create(companyId, registration, request.vehicleType().trim(),
                request.heightMeters(), request.widthMeters(), request.grossWeightKg(),
                request.payloadCapacityKg(), request.maximumEngineTemperatureCelsius(),
                request.refrigerationSupported());
        return toResponse(vehicles.save(vehicle));
    }

    @Transactional
    public VehicleDetailedResponse createDetailed(UUID companyId, UUID actorUserId, UUID traceId, CreateVehicleDetailedRequest request) {
        String registration = request.registrationNumber().trim().toUpperCase(Locale.ROOT);
        if (vehicles.existsByCompanyIdAndRegistrationNumberIgnoreCase(companyId, registration)) {
            throw new ResourceConflictException("VEHICLE_REGISTRATION_EXISTS",
                    "A vehicle with this registration already exists");
        }

        String vin = request.vin() != null && !request.vin().isBlank() ? request.vin().trim().toUpperCase(Locale.ROOT) : null;
        if (vin != null && vehicles.existsByCompanyIdAndVinIgnoreCase(companyId, vin)) {
            throw new ResourceConflictException("VEHICLE_VIN_EXISTS", "A vehicle with this VIN already exists");
        }

        if (request.cargoTemperatureMinCelsius() != null && request.cargoTemperatureMaxCelsius() != null) {
            if (request.cargoTemperatureMinCelsius().compareTo(request.cargoTemperatureMaxCelsius()) > 0) {
                throw new ResourceConflictException("TEMPERATURE_RANGE_INVALID",
                        "Minimum cargo temperature cannot exceed maximum cargo temperature");
            }
        }

        Vehicle vehicle = Vehicle.createDetailed(
                companyId,
                registration,
                vin,
                request.make() != null ? request.make().trim() : null,
                request.model() != null ? request.model().trim() : null,
                request.year(),
                request.vehicleType().trim(),
                request.heightMeters(),
                request.widthMeters(),
                request.grossWeightKg(),
                request.payloadCapacityKg(),
                request.maximumEngineTemperatureCelsius(),
                request.refrigerationSupported(),
                request.cargoTemperatureMinCelsius(),
                request.cargoTemperatureMaxCelsius(),
                request.deviceIdentifier() != null ? request.deviceIdentifier().trim() : null,
                request.status() != null ? request.status() : VehicleStatus.AVAILABLE
        );

        Vehicle saved = vehicles.save(vehicle);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "VEHICLE_CREATED", "VEHICLE", saved.getId(),
                    Map.of("registrationNumber", registration, "vehicleType", saved.getVehicleType()));
        }

        return toDetailedResponse(saved);
    }

    public List<VehicleDetailedResponse> listVehicles(UUID companyId) {
        return vehicles.findAllByCompanyId(companyId).stream()
                .map(this::toDetailedResponse)
                .toList();
    }

    public VehicleResponse get(UUID companyId, UUID vehicleId) {
        return vehicles.findByIdAndCompanyId(vehicleId, companyId).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle was not found"));
    }

    public VehicleDetailedResponse getDetailed(UUID companyId, UUID vehicleId) {
        return vehicles.findByIdAndCompanyId(vehicleId, companyId).map(this::toDetailedResponse)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle was not found"));
    }

    @Transactional
    public VehicleDetailedResponse updateVehicle(UUID companyId, UUID vehicleId, UUID actorUserId, UUID traceId, UpdateVehicleRequest request) {
        Vehicle vehicle = vehicles.findByIdAndCompanyId(vehicleId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle was not found"));

        String registration = request.registrationNumber().trim().toUpperCase(Locale.ROOT);
        if (vehicles.existsByCompanyIdAndRegistrationNumberIgnoreCaseAndIdNot(companyId, registration, vehicleId)) {
            throw new ResourceConflictException("VEHICLE_REGISTRATION_EXISTS",
                    "A vehicle with this registration already exists");
        }

        String vin = request.vin() != null && !request.vin().isBlank() ? request.vin().trim().toUpperCase(Locale.ROOT) : null;
        if (vin != null && vehicles.existsByCompanyIdAndVinIgnoreCaseAndIdNot(companyId, vin, vehicleId)) {
            throw new ResourceConflictException("VEHICLE_VIN_EXISTS", "A vehicle with this VIN already exists");
        }

        if (request.cargoTemperatureMinCelsius() != null && request.cargoTemperatureMaxCelsius() != null) {
            if (request.cargoTemperatureMinCelsius().compareTo(request.cargoTemperatureMaxCelsius()) > 0) {
                throw new ResourceConflictException("TEMPERATURE_RANGE_INVALID",
                        "Minimum cargo temperature cannot exceed maximum cargo temperature");
            }
        }

        vehicle.updateDetails(
                registration,
                vin,
                request.make() != null ? request.make().trim() : null,
                request.model() != null ? request.model().trim() : null,
                request.year(),
                request.vehicleType().trim(),
                request.heightMeters(),
                request.widthMeters(),
                request.grossWeightKg(),
                request.payloadCapacityKg(),
                request.maximumEngineTemperatureCelsius(),
                request.refrigerationSupported(),
                request.cargoTemperatureMinCelsius(),
                request.cargoTemperatureMaxCelsius(),
                request.deviceIdentifier() != null ? request.deviceIdentifier().trim() : null
        );

        Vehicle saved = vehicles.save(vehicle);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "VEHICLE_UPDATED", "VEHICLE", saved.getId(),
                    Map.of("registrationNumber", registration, "vehicleType", saved.getVehicleType()));
        }

        return toDetailedResponse(saved);
    }

    @Transactional
    public VehicleDetailedResponse updateStatus(UUID companyId, UUID vehicleId, UUID actorUserId, UUID traceId, VehicleStatus status) {
        Vehicle vehicle = vehicles.findByIdAndCompanyId(vehicleId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle was not found"));

        vehicle.setStatus(status);
        Vehicle saved = vehicles.save(vehicle);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "VEHICLE_STATUS_CHANGED", "VEHICLE", saved.getId(),
                    Map.of("status", status.name()));
        }

        return toDetailedResponse(saved);
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getRegistrationNumber(), vehicle.getVehicleType(),
                vehicle.getHeightMeters(), vehicle.getWidthMeters(), vehicle.getGrossWeightKg(),
                vehicle.getPayloadCapacityKg(), vehicle.getMaximumEngineTemperatureCelsius(),
                vehicle.isRefrigerationSupported(), vehicle.getStatus(), vehicle.getCurrentEngineHealthPercent(),
                vehicle.getCreatedAt(), vehicle.getUpdatedAt());
    }

    private VehicleDetailedResponse toDetailedResponse(Vehicle vehicle) {
        UUID assignedDriverId = null;
        String assignedDriverName = null;

        if (assignments != null) {
            Optional<DriverVehicleAssignment> assignment = assignments.findFirstByCompanyIdAndVehicleIdAndStatus(
                    vehicle.getCompanyId(), vehicle.getId(), "ACTIVE");
            if (assignment.isPresent()) {
                assignedDriverId = assignment.get().getDriverId();
                if (drivers != null) {
                    assignedDriverName = drivers.findById(assignedDriverId)
                            .map(d -> (d.getFirstName() != null ? d.getFirstName() + " " + (d.getLastName() != null ? d.getLastName() : "") : d.getLicenseNumber()).trim())
                            .orElse(null);
                }
            }
        }

        return new VehicleDetailedResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getVin(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getVehicleType(),
                vehicle.getStatus(),
                vehicle.getHeightMeters(),
                vehicle.getWidthMeters(),
                vehicle.getGrossWeightKg(),
                vehicle.getPayloadCapacityKg(),
                vehicle.getMaximumEngineTemperatureCelsius(),
                vehicle.isRefrigerationSupported(),
                vehicle.getCargoTemperatureMinCelsius(),
                vehicle.getCargoTemperatureMaxCelsius(),
                vehicle.getDeviceIdentifier(),
                vehicle.getCurrentEngineHealthPercent(),
                assignedDriverId,
                assignedDriverName,
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}
