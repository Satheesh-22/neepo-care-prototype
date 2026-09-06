package com.neepo.backend.service;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.CreateDriverRequest;
import com.neepo.backend.dto.DriverResponse;
import com.neepo.backend.entity.Driver;
import com.neepo.backend.entity.User;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DriverService {
    private final DriverRepository drivers;
    private final UserRepository users;

    public DriverService(DriverRepository drivers, UserRepository users) {
        this.drivers = drivers;
        this.users = users;
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
        return toResponse(drivers.save(Driver.create(companyId, user.getId(), license,
                request.maximumDailyDrivingMinutes())));
    }

    public DriverResponse get(UUID companyId, UUID driverId) {
        return drivers.findByIdAndCompanyId(driverId, companyId).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));
    }

    private DriverResponse toResponse(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getUserId(), driver.getLicenseNumber(), driver.getStatus(),
                driver.getMaximumDailyDrivingMinutes(), driver.getRemainingDrivingMinutes(),
                driver.getLastRestCompletedAt(), driver.getCreatedAt(), driver.getUpdatedAt());
    }
}
