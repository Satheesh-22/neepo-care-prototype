package com.neepo.backend.repository;

import com.neepo.backend.entity.DriverVehicleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverVehicleAssignmentRepository extends JpaRepository<DriverVehicleAssignment, UUID> {
    List<DriverVehicleAssignment> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    Optional<DriverVehicleAssignment> findByIdAndCompanyId(UUID id, UUID companyId);
    Optional<DriverVehicleAssignment> findFirstByCompanyIdAndDriverIdAndStatus(UUID companyId, UUID driverId, String status);
    Optional<DriverVehicleAssignment> findFirstByCompanyIdAndVehicleIdAndStatus(UUID companyId, UUID vehicleId, String status);
    List<DriverVehicleAssignment> findAllByCompanyIdAndDriverId(UUID companyId, UUID driverId);
    List<DriverVehicleAssignment> findAllByCompanyIdAndVehicleId(UUID companyId, UUID vehicleId);

    @Query("SELECT COUNT(a) > 0 FROM DriverVehicleAssignment a WHERE a.companyId = :companyId AND a.driverId = :driverId AND a.status = 'ACTIVE'")
    boolean hasActiveAssignmentForDriver(UUID companyId, UUID driverId);

    @Query("SELECT COUNT(a) > 0 FROM DriverVehicleAssignment a WHERE a.companyId = :companyId AND a.vehicleId = :vehicleId AND a.status = 'ACTIVE'")
    boolean hasActiveAssignmentForVehicle(UUID companyId, UUID vehicleId);
}
