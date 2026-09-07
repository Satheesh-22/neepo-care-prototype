package com.neepo.backend.service;

import com.neepo.backend.domain.DriverStatus;
import com.neepo.backend.domain.VehicleStatus;
import com.neepo.backend.dto.DriverVehicleAssignmentRequest;
import com.neepo.backend.dto.DriverVehicleAssignmentResponse;
import com.neepo.backend.entity.Driver;
import com.neepo.backend.entity.DriverVehicleAssignment;
import com.neepo.backend.entity.User;
import com.neepo.backend.entity.Vehicle;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.DriverVehicleAssignmentRepository;
import com.neepo.backend.repository.UserRepository;
import com.neepo.backend.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final DriverVehicleAssignmentRepository assignmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    public AssignmentService(DriverVehicleAssignmentRepository assignmentRepository,
                             DriverRepository driverRepository,
                             VehicleRepository vehicleRepository,
                             UserRepository userRepository,
                             AuditService auditService,
                             Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    public List<DriverVehicleAssignmentResponse> listAssignments(UUID companyId) {
        return assignmentRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DriverVehicleAssignmentResponse assign(UUID companyId, UUID actorUserId, UUID traceId,
                                                 DriverVehicleAssignmentRequest request) {
        Driver driver = driverRepository.findByIdAndCompanyId(request.driverId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found in this company"));

        Vehicle vehicle = vehicleRepository.findByIdAndCompanyId(request.vehicleId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle was not found in this company"));

        // Validation rules
        if (driver.isSuspended() || driver.getStatus() == DriverStatus.OFF_DUTY) {
            throw new ResourceConflictException("DRIVER_SUSPENDED", "Cannot assign a suspended or off-duty driver");
        }

        if (driver.isLicenseExpired(LocalDate.now(clock))) {
            throw new ResourceConflictException("DRIVER_LICENSE_EXPIRED", "Cannot assign driver with an expired license");
        }

        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE || vehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE) {
            throw new ResourceConflictException("VEHICLE_UNAVAILABLE",
                    "Cannot assign a vehicle that is in maintenance or out of service");
        }

        if (assignmentRepository.hasActiveAssignmentForDriver(companyId, driver.getId())) {
            throw new ResourceConflictException("DRIVER_ALREADY_ASSIGNED",
                    "Driver already has an active vehicle assignment");
        }

        if (assignmentRepository.hasActiveAssignmentForVehicle(companyId, vehicle.getId())) {
            throw new ResourceConflictException("VEHICLE_ALREADY_ASSIGNED",
                    "Vehicle is already assigned to an active driver");
        }

        Instant startTime = request.startTime() != null ? request.startTime() : clock.instant();

        DriverVehicleAssignment assignment = DriverVehicleAssignment.create(
                companyId,
                driver.getId(),
                vehicle.getId(),
                startTime,
                request.endTime(),
                request.notes() != null ? request.notes().trim() : null,
                actorUserId
        );

        DriverVehicleAssignment saved = assignmentRepository.save(assignment);

        driver.markAssigned();
        driverRepository.save(driver);

        vehicle.markAssigned();
        vehicleRepository.save(vehicle);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "DRIVER_VEHICLE_ASSIGNED", "ASSIGNMENT", saved.getId(),
                    Map.of("driverId", driver.getId().toString(),
                            "vehicleId", vehicle.getId().toString(),
                            "vehicleRegistration", vehicle.getRegistrationNumber()));
        }

        return toResponse(saved);
    }

    @Transactional
    public DriverVehicleAssignmentResponse release(UUID companyId, UUID assignmentId, UUID actorUserId, UUID traceId) {
        DriverVehicleAssignment assignment = assignmentRepository.findByIdAndCompanyId(assignmentId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("ASSIGNMENT_NOT_FOUND", "Assignment was not found"));

        if (!"ACTIVE".equals(assignment.getStatus())) {
            throw new ResourceConflictException("ASSIGNMENT_NOT_ACTIVE", "Assignment is already released or cancelled");
        }

        Instant now = clock.instant();
        assignment.release(now);
        DriverVehicleAssignment saved = assignmentRepository.save(assignment);

        driverRepository.findById(assignment.getDriverId()).ifPresent(driver -> {
            if (driver.getStatus() == DriverStatus.ASSIGNED) {
                driver.markAvailable();
                driverRepository.save(driver);
            }
        });

        vehicleRepository.findById(assignment.getVehicleId()).ifPresent(vehicle -> {
            if (vehicle.getStatus() == VehicleStatus.ASSIGNED) {
                vehicle.markAvailable();
                vehicleRepository.save(vehicle);
            }
        });

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "DRIVER_VEHICLE_RELEASED", "ASSIGNMENT", saved.getId(),
                    Map.of("driverId", assignment.getDriverId().toString(),
                            "vehicleId", assignment.getVehicleId().toString()));
        }

        return toResponse(saved);
    }

    private DriverVehicleAssignmentResponse toResponse(DriverVehicleAssignment a) {
        String driverName = "Unknown";
        String driverLicense = "Unknown";
        String driverEmpId = null;
        Optional<Driver> driverOpt = driverRepository.findById(a.getDriverId());
        if (driverOpt.isPresent()) {
            Driver d = driverOpt.get();
            driverLicense = d.getLicenseNumber();
            driverEmpId = d.getEmployeeId();
            if (d.getFirstName() != null) {
                driverName = (d.getFirstName() + " " + (d.getLastName() != null ? d.getLastName() : "")).trim();
            } else {
                userRepository.findById(d.getUserId()).ifPresent(u -> {
                    // Fallback to User name
                });
            }
        }

        String vehicleReg = "Unknown";
        String vehicleType = "Unknown";
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(a.getVehicleId());
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();
            vehicleReg = v.getRegistrationNumber();
            vehicleType = v.getVehicleType();
        }

        return new DriverVehicleAssignmentResponse(
                a.getId(),
                a.getCompanyId(),
                a.getDriverId(),
                driverName,
                driverLicense,
                driverEmpId,
                a.getVehicleId(),
                vehicleReg,
                vehicleType,
                a.getStartTime(),
                a.getEndTime(),
                a.getNotes(),
                a.getStatus(),
                a.getAssignedByUserId(),
                a.getCreatedAt(),
                a.getReleasedAt()
        );
    }
}
