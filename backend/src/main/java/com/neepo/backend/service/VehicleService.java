package com.neepo.backend.service;

import com.neepo.backend.dto.CreateVehicleRequest;
import com.neepo.backend.dto.VehicleResponse;
import com.neepo.backend.entity.Vehicle;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicles;

    public VehicleService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
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

    public VehicleResponse get(UUID companyId, UUID vehicleId) {
        return vehicles.findByIdAndCompanyId(vehicleId, companyId).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle was not found"));
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getRegistrationNumber(), vehicle.getVehicleType(),
                vehicle.getHeightMeters(), vehicle.getWidthMeters(), vehicle.getGrossWeightKg(),
                vehicle.getPayloadCapacityKg(), vehicle.getMaximumEngineTemperatureCelsius(),
                vehicle.isRefrigerationSupported(), vehicle.getStatus(), vehicle.getCurrentEngineHealthPercent(),
                vehicle.getCreatedAt(), vehicle.getUpdatedAt());
    }
}
