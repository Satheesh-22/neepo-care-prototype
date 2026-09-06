package com.neepo.backend.repository;

import com.neepo.backend.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndRegistrationNumberIgnoreCase(UUID companyId, String registrationNumber);
}
