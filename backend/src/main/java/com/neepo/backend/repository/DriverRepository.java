package com.neepo.backend.repository;

import com.neepo.backend.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Optional<Driver> findByIdAndCompanyId(UUID id, UUID companyId);
    Optional<Driver> findByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByCompanyIdAndLicenseNumberIgnoreCase(UUID companyId, String licenseNumber);
    boolean existsByUserId(UUID userId);
}
