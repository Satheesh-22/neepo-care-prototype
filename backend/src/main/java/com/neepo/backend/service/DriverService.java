package com.neepo.backend.service;

import com.neepo.backend.domain.DriverStatus;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.CreateDriverDetailedRequest;
import com.neepo.backend.dto.CreateDriverRequest;
import com.neepo.backend.dto.DriverDetailedResponse;
import com.neepo.backend.dto.DriverResponse;
import com.neepo.backend.dto.UpdateDriverRequest;
import com.neepo.backend.dto.UserInvitationResponse;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DriverService {
    private final DriverRepository drivers;
    private final UserRepository users;
    private final VehicleRepository vehicles;
    private final DriverVehicleAssignmentRepository assignments;
    private final UserInvitationService invitationService;
    private final AuditService auditService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DriverService(DriverRepository drivers,
                         UserRepository users,
                         VehicleRepository vehicles,
                         DriverVehicleAssignmentRepository assignments,
                         UserInvitationService invitationService,
                         AuditService auditService,
                         Clock clock) {
        this.drivers = drivers;
        this.users = users;
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.invitationService = invitationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    public DriverService(DriverRepository drivers, UserRepository users) {
        this(drivers, users, null, null, null, null, Clock.systemUTC());
    }

    @Transactional
    public DriverResponse create(UUID companyId, CreateDriverRequest request) {
        User user = users.findById(request.userId())
                .filter(candidate -> companyId.equals(candidate.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_USER_NOT_FOUND",
                        "Driver user was not found in this company"));
        if (user.getRole() != UserRole.DRIVER) {
            throw new ResourceConflictException("USER_ROLE_NOT_DRIVER", "The selected user does not have DRIVER role");
        }
        if (drivers.existsByUserId(user.getId())) {
            throw new ResourceConflictException("DRIVER_USER_EXISTS", "The selected user already has a driver profile");
        }
        String license = request.licenseNumber().trim().toUpperCase(Locale.ROOT);
        if (drivers.existsByCompanyIdAndLicenseNumberIgnoreCase(companyId, license)) {
            throw new ResourceConflictException("DRIVER_LICENSE_EXISTS", "This license is already registered");
        }
        Driver driver = Driver.create(companyId, user.getId(), license, request.maximumDailyDrivingMinutes());
        driver = drivers.save(driver);
        return toResponse(driver);
    }

    @Transactional
    public DriverDetailedResponse createDetailed(UUID companyId, UUID actorUserId, UUID traceId, CreateDriverDetailedRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String license = request.licenseNumber().trim().toUpperCase(Locale.ROOT);
        String employeeId = request.employeeId().trim().toUpperCase(Locale.ROOT);

        if (drivers.existsByCompanyIdAndEmployeeIdIgnoreCase(companyId, employeeId)) {
            throw new ResourceConflictException("DRIVER_EMPLOYEE_ID_EXISTS", "A driver with this employee ID already exists");
        }
        if (drivers.existsByCompanyIdAndLicenseNumberIgnoreCase(companyId, license)) {
            throw new ResourceConflictException("DRIVER_LICENSE_EXISTS", "A driver with this license number already exists");
        }

        if (request.licenseExpiryDate() != null && request.licenseExpiryDate().isBefore(LocalDate.now(clock))) {
            throw new ResourceConflictException("DRIVER_LICENSE_EXPIRED", "License expiry date cannot be in the past");
        }

        String driverName = (request.firstName().trim() + " " + request.lastName().trim()).trim();
        if (request.emergencyContactName() != null && request.emergencyContactName().trim().equalsIgnoreCase(driverName)) {
            throw new ResourceConflictException("EMERGENCY_CONTACT_INVALID", "Emergency contact cannot be the driver");
        }

        // Create user account or get invited user
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        String invitationToken = null;
        if (user == null) {
            user = User.createInvited(companyId, driverName, email, request.phone().trim(), UserRole.DRIVER);
            user = users.save(user);

            if (invitationService != null) {
                UserInvitationResponse inv = invitationService.inviteUser(
                        companyId, actorUserId, UserRole.ADMIN, traceId,
                        new com.neepo.backend.dto.CreateUserInvitationRequest(driverName, email, request.phone().trim(), UserRole.DRIVER)
                );
                invitationToken = inv.token();
            }
        } else {
            if (!companyId.equals(user.getCompanyId())) {
                throw new ResourceConflictException("USER_EMAIL_EXISTS_OTHER_COMPANY", "User email belongs to another company");
            }
            if (drivers.existsByUserId(user.getId())) {
                throw new ResourceConflictException("DRIVER_USER_EXISTS", "A driver profile already exists for this email");
            }
        }

        Driver driver = Driver.createDetailed(
                companyId,
                user.getId(),
                employeeId,
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.phone().trim(),
                license,
                request.licenseCategory(),
                request.licenseExpiryDate(),
                request.emergencyContactName() != null ? request.emergencyContactName().trim() : null,
                request.emergencyContactPhone() != null ? request.emergencyContactPhone().trim() : null,
                request.baseLocation() != null ? request.baseLocation().trim() : null,
                request.employmentStatus(),
                request.profilePhotoUrl() != null ? request.profilePhotoUrl().trim() : null,
                "VERIFIED",
                request.maximumDailyDrivingMinutes() != null && request.maximumDailyDrivingMinutes() > 0 ? request.maximumDailyDrivingMinutes() : 600
        );

        Driver saved = drivers.save(driver);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "DRIVER_CREATED", "DRIVER", saved.getId(),
                    Map.of("employeeId", employeeId, "email", email, "licenseNumber", license));
        }

        return toDetailedResponse(saved, invitationToken);
    }

    public List<DriverDetailedResponse> listDrivers(UUID companyId) {
        return drivers.findAllByCompanyId(companyId).stream()
                .map(d -> toDetailedResponse(d, null))
                .toList();
    }

    public DriverResponse get(UUID companyId, UUID driverId) {
        return drivers.findByIdAndCompanyId(driverId, companyId).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));
    }

    public DriverDetailedResponse getDetailed(UUID companyId, UUID driverId) {
        Driver driver = drivers.findByIdAndCompanyId(driverId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));
        return toDetailedResponse(driver, null);
    }

    public DriverDetailedResponse getByUserId(UUID companyId, UUID userId) {
        Driver driver = drivers.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver profile not found for user"));
        return toDetailedResponse(driver, null);
    }

    @Transactional
    public DriverDetailedResponse updateDriver(UUID companyId, UUID driverId, UUID actorUserId, UUID traceId, UpdateDriverRequest request) {
        Driver driver = drivers.findByIdAndCompanyId(driverId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));

        String employeeId = request.employeeId().trim().toUpperCase(Locale.ROOT);
        String license = request.licenseNumber().trim().toUpperCase(Locale.ROOT);

        if (drivers.existsByCompanyIdAndEmployeeIdIgnoreCaseAndIdNot(companyId, employeeId, driverId)) {
            throw new ResourceConflictException("DRIVER_EMPLOYEE_ID_EXISTS", "Employee ID is already in use by another driver");
        }
        if (drivers.existsByCompanyIdAndLicenseNumberIgnoreCaseAndIdNot(companyId, license, driverId)) {
            throw new ResourceConflictException("DRIVER_LICENSE_EXISTS", "License number is already in use by another driver");
        }

        String driverName = (request.firstName().trim() + " " + request.lastName().trim()).trim();
        if (request.emergencyContactName() != null && request.emergencyContactName().trim().equalsIgnoreCase(driverName)) {
            throw new ResourceConflictException("EMERGENCY_CONTACT_INVALID", "Emergency contact cannot be the driver");
        }

        driver.updateProfile(
                employeeId,
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                license,
                request.licenseCategory(),
                request.licenseExpiryDate(),
                request.emergencyContactName(),
                request.emergencyContactPhone(),
                request.baseLocation(),
                request.employmentStatus(),
                request.profilePhotoUrl(),
                request.maximumDailyDrivingMinutes() != null && request.maximumDailyDrivingMinutes() > 0 ? request.maximumDailyDrivingMinutes() : driver.getMaximumDailyDrivingMinutes()
        );

        Driver saved = drivers.save(driver);

        // Also update name on user record
        users.findById(driver.getUserId()).ifPresent(user -> {
            user.updateDetails(driverName, request.phone().trim(), user.getRole());
            users.save(user);
        });

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "DRIVER_UPDATED", "DRIVER", saved.getId(),
                    Map.of("employeeId", employeeId, "licenseNumber", license));
        }

        return toDetailedResponse(saved, null);
    }

    @Transactional
    public DriverDetailedResponse suspendDriver(UUID companyId, UUID driverId, UUID actorUserId, UUID traceId) {
        Driver driver = drivers.findByIdAndCompanyId(driverId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));

        driver.suspend();
        Driver saved = drivers.save(driver);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "DRIVER_SUSPENDED", "DRIVER", saved.getId(),
                    Map.of("employeeId", saved.getEmployeeId() != null ? saved.getEmployeeId() : ""));
        }

        return toDetailedResponse(saved, null);
    }

    @Transactional
    public DriverDetailedResponse reactivateDriver(UUID companyId, UUID driverId, UUID actorUserId, UUID traceId) {
        Driver driver = drivers.findByIdAndCompanyId(driverId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));

        driver.reactivate();
        Driver saved = drivers.save(driver);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "DRIVER_REACTIVATED", "DRIVER", saved.getId(),
                    Map.of("employeeId", saved.getEmployeeId() != null ? saved.getEmployeeId() : ""));
        }

        return toDetailedResponse(saved, null);
    }

    @Transactional
    public UserInvitationResponse inviteDriver(UUID companyId, UUID driverId, UUID actorUserId, UUID traceId) {
        Driver driver = drivers.findByIdAndCompanyId(driverId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));

        User user = users.findById(driver.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User account not found"));

        if (invitationService == null) {
            throw new IllegalStateException("Invitation service not available");
        }

        String driverName = driver.getFirstName() != null ? driver.getFirstName() + " " + driver.getLastName() : user.getName();
        return invitationService.inviteUser(
                companyId, actorUserId, UserRole.ADMIN, traceId,
                new com.neepo.backend.dto.CreateUserInvitationRequest(driverName, user.getEmail(), user.getPhone(), UserRole.DRIVER)
        );
    }

    private DriverResponse toResponse(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getUserId(), driver.getLicenseNumber(), driver.getStatus(),
                driver.getMaximumDailyDrivingMinutes(), driver.getRemainingDrivingMinutes(),
                driver.getLastRestCompletedAt(), driver.getCreatedAt(), driver.getUpdatedAt());
    }

    private DriverDetailedResponse toDetailedResponse(Driver driver, String invitationToken) {
        String firstName = driver.getFirstName();
        String lastName = driver.getLastName();
        String email = driver.getEmail();
        String phone = driver.getPhone();
        Instant lastActive = driver.getLastActiveAt();

        if (firstName == null || email == null) {
            Optional<User> u = users.findById(driver.getUserId());
            if (u.isPresent()) {
                User user = u.get();
                if (email == null) email = user.getEmail();
                if (phone == null) phone = user.getPhone();
                if (firstName == null) {
                    firstName = user.getName();
                    lastName = "";
                }
                if (lastActive == null) lastActive = user.getLastActiveAt();
            }
        }

        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        boolean licenseExpired = driver.isLicenseExpired(LocalDate.now(clock));

        UUID assignedVehicleId = null;
        String assignedVehicleRegistration = null;
        if (assignments != null) {
            Optional<DriverVehicleAssignment> assignment = assignments.findFirstByCompanyIdAndDriverIdAndStatus(
                    driver.getCompanyId(), driver.getId(), "ACTIVE");
            if (assignment.isPresent()) {
                assignedVehicleId = assignment.get().getVehicleId();
                if (vehicles != null) {
                    assignedVehicleRegistration = vehicles.findById(assignedVehicleId)
                            .map(Vehicle::getRegistrationNumber)
                            .orElse(null);
                }
            }
        }

        return new DriverDetailedResponse(
                driver.getId(),
                driver.getUserId(),
                driver.getEmployeeId(),
                firstName,
                lastName,
                fullName,
                email,
                phone,
                driver.getLicenseNumber(),
                driver.getLicenseCategory(),
                driver.getLicenseExpiryDate(),
                licenseExpired,
                driver.getEmergencyContactName(),
                driver.getEmergencyContactPhone(),
                driver.getBaseLocation(),
                driver.getEmploymentStatus(),
                driver.getProfilePhotoUrl(),
                driver.getVerificationState(),
                driver.getStatus(),
                driver.getMaximumDailyDrivingMinutes(),
                driver.getRemainingDrivingMinutes(),
                driver.getLastRestCompletedAt(),
                lastActive,
                assignedVehicleId,
                assignedVehicleRegistration,
                driver.getCreatedAt(),
                driver.getUpdatedAt(),
                invitationToken
        );
    }
}
