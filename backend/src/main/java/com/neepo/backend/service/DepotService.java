package com.neepo.backend.service;

import com.neepo.backend.domain.DepotStatus;
import com.neepo.backend.dto.CreateDepotRequest;
import com.neepo.backend.dto.DepotResponse;
import com.neepo.backend.dto.UpdateDepotRequest;
import com.neepo.backend.entity.Depot;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DepotRepository;
import com.neepo.backend.repository.UserRepository;
import com.neepo.backend.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepotService {

    private final DepotRepository depotRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public DepotService(
            DepotRepository depotRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository
    ) {
        this.depotRepository = depotRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DepotResponse> listDepots(UUID companyId) {
        return depotRepository.findAllByCompanyIdOrderByCodeAsc(companyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepotResponse getDepot(UUID companyId, UUID depotId) {
        Depot depot = depotRepository.findByIdAndCompanyId(depotId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DEPOT_NOT_FOUND", "Depot not found with id: " + depotId));
        return toResponse(depot);
    }

    public DepotResponse createDepot(UUID companyId, CreateDepotRequest request) {
        String code = request.code().trim().toUpperCase();
        if (depotRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new ResourceConflictException("DEPOT_CODE_EXISTS", "A depot with code '" + code + "' already exists in this organization.");
        }

        Depot depot = new Depot();
        depot.setCompanyId(companyId);
        depot.setCode(code);
        depot.setName(request.name().trim());
        depot.setAddress(request.address().trim());
        depot.setCity(request.city().trim());
        depot.setState(request.state() != null ? request.state().trim() : null);
        depot.setPostalCode(request.postalCode() != null ? request.postalCode().trim() : null);
        depot.setLatitude(request.latitude());
        depot.setLongitude(request.longitude());
        depot.setCapacity(request.capacity() != null ? request.capacity() : 50);
        depot.setStatus(request.status() != null ? request.status() : DepotStatus.ACTIVE);
        depot.setContactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null);
        depot.setContactEmail(request.contactEmail() != null ? request.contactEmail().trim() : null);

        Depot saved = depotRepository.save(depot);
        return toResponse(saved);
    }

    public DepotResponse updateDepot(UUID companyId, UUID depotId, UpdateDepotRequest request) {
        Depot depot = depotRepository.findByIdAndCompanyId(depotId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DEPOT_NOT_FOUND", "Depot not found with id: " + depotId));

        if (request.code() != null && !request.code().isBlank()) {
            String code = request.code().trim().toUpperCase();
            if (depotRepository.existsByCompanyIdAndCodeIgnoreCaseAndIdNot(companyId, code, depotId)) {
                throw new ResourceConflictException("DEPOT_CODE_EXISTS", "A depot with code '" + code + "' already exists in this organization.");
            }
            depot.setCode(code);
        }

        if (request.name() != null && !request.name().isBlank()) {
            depot.setName(request.name().trim());
        }
        if (request.address() != null && !request.address().isBlank()) {
            depot.setAddress(request.address().trim());
        }
        if (request.city() != null && !request.city().isBlank()) {
            depot.setCity(request.city().trim());
        }
        if (request.state() != null) {
            depot.setState(request.state().trim());
        }
        if (request.postalCode() != null) {
            depot.setPostalCode(request.postalCode().trim());
        }
        if (request.latitude() != null) {
            depot.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            depot.setLongitude(request.longitude());
        }
        if (request.capacity() != null) {
            depot.setCapacity(request.capacity());
        }
        if (request.status() != null) {
            depot.setStatus(request.status());
        }
        if (request.contactPhone() != null) {
            depot.setContactPhone(request.contactPhone().trim());
        }
        if (request.contactEmail() != null) {
            depot.setContactEmail(request.contactEmail().trim());
        }

        Depot updated = depotRepository.save(depot);
        return toResponse(updated);
    }

    public void deleteDepot(UUID companyId, UUID depotId) {
        Depot depot = depotRepository.findByIdAndCompanyId(depotId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DEPOT_NOT_FOUND", "Depot not found with id: " + depotId));

        // Detach vehicles and users linked to this depot
        vehicleRepository.findAllByCompanyIdAndDepotId(companyId, depotId)
                .forEach(v -> {
                    v.setDepotId(null);
                    vehicleRepository.save(v);
                });
        userRepository.findAllByCompanyIdAndDepotId(companyId, depotId)
                .forEach(u -> {
                    u.setDepotId(null);
                    userRepository.save(u);
                });

        depotRepository.delete(depot);
    }

    private DepotResponse toResponse(Depot depot) {
        long vehiclesCount = vehicleRepository.countByCompanyIdAndDepotId(depot.getCompanyId(), depot.getId());
        long driversCount = userRepository.countByCompanyIdAndDepotId(depot.getCompanyId(), depot.getId());

        return new DepotResponse(
                depot.getId(),
                depot.getCompanyId(),
                depot.getCode(),
                depot.getName(),
                depot.getAddress(),
                depot.getCity(),
                depot.getState(),
                depot.getPostalCode(),
                depot.getLatitude(),
                depot.getLongitude(),
                depot.getCapacity(),
                depot.getStatus(),
                depot.getContactPhone(),
                depot.getContactEmail(),
                vehiclesCount,
                driversCount,
                depot.getCreatedAt(),
                depot.getUpdatedAt()
        );
    }
}

