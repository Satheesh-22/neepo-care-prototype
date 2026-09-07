package com.neepo.backend.repository;

import com.neepo.backend.entity.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Optional<Driver> findByIdAndCompanyId(UUID id, UUID companyId);
    Optional<Driver> findByUserIdAndCompanyId(UUID userId, UUID companyId);
    Optional<Driver> findByUserId(UUID userId);
    List<Driver> findAllByCompanyId(UUID companyId);
    Page<Driver> findAllByCompanyId(UUID companyId, Pageable pageable);
    boolean existsByCompanyIdAndLicenseNumberIgnoreCase(UUID companyId, String licenseNumber);
    boolean existsByCompanyIdAndLicenseNumberIgnoreCaseAndIdNot(UUID companyId, String licenseNumber, UUID id);
    boolean existsByCompanyIdAndEmployeeIdIgnoreCase(UUID companyId, String employeeId);
    boolean existsByCompanyIdAndEmployeeIdIgnoreCaseAndIdNot(UUID companyId, String employeeId, UUID id);
    boolean existsByUserId(UUID userId);
}
