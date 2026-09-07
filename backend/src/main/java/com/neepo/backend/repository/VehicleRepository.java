package com.neepo.backend.repository;

import com.neepo.backend.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByIdAndCompanyId(UUID id, UUID companyId);
    List<Vehicle> findAllByCompanyId(UUID companyId);
    boolean existsByCompanyIdAndRegistrationNumberIgnoreCase(UUID companyId, String registrationNumber);
    boolean existsByCompanyIdAndRegistrationNumberIgnoreCaseAndIdNot(UUID companyId, String registrationNumber, UUID id);
    boolean existsByCompanyIdAndVinIgnoreCase(UUID companyId, String vin);
    boolean existsByCompanyIdAndVinIgnoreCaseAndIdNot(UUID companyId, String vin, UUID id);
}
