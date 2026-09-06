package com.neepo.backend.service;

import com.neepo.backend.dto.CargoResponse;
import com.neepo.backend.dto.CreateCargoRequest;
import com.neepo.backend.entity.Cargo;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.CargoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CargoService {
    private final CargoRepository cargo;
    public CargoService(CargoRepository cargo) { this.cargo = cargo; }

    @Transactional
    public CargoResponse create(UUID companyId, CreateCargoRequest request) {
        String reference = request.referenceCode().trim().toUpperCase(Locale.ROOT);
        if (cargo.existsByCompanyIdAndReferenceCodeIgnoreCase(companyId, reference)) {
            throw new ResourceConflictException("CARGO_REFERENCE_EXISTS", "Cargo reference already exists");
        }
        Cargo entity = Cargo.create(companyId, reference, request.cargoType(), request.description().trim(),
                request.weightKg(), request.minimumTemperatureCelsius(), request.maximumTemperatureCelsius(),
                request.hazardous());
        return toResponse(cargo.save(entity));
    }

    public CargoResponse get(UUID companyId, UUID cargoId) {
        return cargo.findByIdAndCompanyId(cargoId, companyId).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("CARGO_NOT_FOUND", "Cargo was not found"));
    }

    private CargoResponse toResponse(Cargo entity) {
        return new CargoResponse(entity.getId(), entity.getReferenceCode(), entity.getCargoType(),
                entity.getDescription(), entity.getWeightKg(), entity.getMinimumTemperatureCelsius(),
                entity.getMaximumTemperatureCelsius(), entity.isHazardous(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
