package com.neepo.backend.repository;

import com.neepo.backend.entity.HardwareGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HardwareGatewayRepository extends JpaRepository<HardwareGateway, UUID> {
    List<HardwareGateway> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    Optional<HardwareGateway> findByIdAndCompanyId(UUID id, UUID companyId);
    Optional<HardwareGateway> findByCompanyIdAndDeviceSerial(UUID companyId, String deviceSerial);
    Optional<HardwareGateway> findByDeviceSerial(String deviceSerial);
    Optional<HardwareGateway> findByCompanyIdAndVehicleId(UUID companyId, UUID vehicleId);
}
