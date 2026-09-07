package com.neepo.backend.service;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.CompanyResponse;
import com.neepo.backend.dto.UpdateCompanyRequest;
import com.neepo.backend.entity.Company;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final AuditService auditService;

    public CompanyService(CompanyRepository companyRepository, AuditService auditService) {
        this.companyRepository = companyRepository;
        this.auditService = auditService;
    }

    public CompanyResponse getCompany(UUID companyId, UserRole role) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("COMPANY_NOT_FOUND", "Company was not found"));
        return sanitizeForRole(company, role);
    }

    @Transactional
    public CompanyResponse updateCompany(UUID companyId, UUID actorUserId, UUID traceId, UpdateCompanyRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("COMPANY_NOT_FOUND", "Company was not found"));

        String regNumber = request.registrationNumber().trim().toUpperCase(Locale.ROOT);
        if (companyRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(regNumber, companyId)) {
            throw new ResourceConflictException("REGISTRATION_NUMBER_EXISTS", "Registration number is already in use by another company");
        }

        company.updateProfile(
                request.legalName().trim(),
                request.tradingName() != null ? request.tradingName().trim() : null,
                regNumber,
                request.industry() != null ? request.industry().trim() : null,
                request.address() != null ? request.address().trim() : null,
                request.city() != null ? request.city().trim() : null,
                request.stateProvince() != null ? request.stateProvince().trim() : null,
                request.country() != null ? request.country().trim() : null,
                request.postalCode() != null ? request.postalCode().trim() : null,
                request.contactPerson() != null ? request.contactPerson().trim() : null,
                request.businessEmail().trim().toLowerCase(Locale.ROOT),
                request.phoneNumber() != null ? request.phoneNumber().trim() : null,
                request.timezone() != null ? request.timezone().trim() : "UTC",
                request.measurementUnits() != null ? request.measurementUnits().trim() : "METRIC",
                request.cargoTemperaturePolicy() != null ? request.cargoTemperaturePolicy().trim() : "STRICT_COLD_CHAIN",
                request.logoReference() != null ? request.logoReference().trim() : null,
                request.dataRetentionDays() != null && request.dataRetentionDays() > 0 ? request.dataRetentionDays() : company.getDataRetentionDays()
        );

        Company saved = companyRepository.save(company);

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("legalName", saved.getLegalName());
        auditDetails.put("registrationNumber", saved.getRegistrationNumber());
        auditDetails.put("businessEmail", saved.getBusinessEmail());
        auditDetails.put("timezone", saved.getTimezone());
        auditDetails.put("measurementUnits", saved.getMeasurementUnits());
        auditDetails.put("cargoTemperaturePolicy", saved.getCargoTemperaturePolicy());

        auditService.record(traceId, actorUserId, "COMPANY_PROFILE_UPDATED", "COMPANY", companyId, auditDetails);

        return sanitizeForRole(saved, UserRole.ADMIN);
    }

    private CompanyResponse sanitizeForRole(Company company, UserRole role) {
        if (role == UserRole.DRIVER) {
            // Minimal information required for mobile/trip work
            return new CompanyResponse(
                    company.getId(),
                    company.getCode(),
                    company.getName(),
                    null,
                    company.getTradingName(),
                    null,
                    null,
                    null,
                    company.getCity(),
                    company.getStateProvince(),
                    company.getCountry(),
                    null,
                    company.getContactPerson(),
                    null,
                    company.getPhoneNumber(),
                    company.getTimezone(),
                    company.getMeasurementUnits(),
                    company.getCargoTemperaturePolicy(),
                    company.getLogoReference(),
                    0,
                    company.getCreatedAt(),
                    company.getUpdatedAt()
            );
        } else if (role == UserRole.DISPATCHER) {
            // Operational company information
            return new CompanyResponse(
                    company.getId(),
                    company.getCode(),
                    company.getName(),
                    company.getLegalName(),
                    company.getTradingName(),
                    company.getRegistrationNumber(),
                    company.getIndustry(),
                    company.getAddress(),
                    company.getCity(),
                    company.getStateProvince(),
                    company.getCountry(),
                    company.getPostalCode(),
                    company.getContactPerson(),
                    company.getBusinessEmail(),
                    company.getPhoneNumber(),
                    company.getTimezone(),
                    company.getMeasurementUnits(),
                    company.getCargoTemperaturePolicy(),
                    company.getLogoReference(),
                    company.getDataRetentionDays(),
                    company.getCreatedAt(),
                    company.getUpdatedAt()
            );
        } else {
            // ADMIN gets full profile
            return new CompanyResponse(
                    company.getId(),
                    company.getCode(),
                    company.getName(),
                    company.getLegalName(),
                    company.getTradingName(),
                    company.getRegistrationNumber(),
                    company.getIndustry(),
                    company.getAddress(),
                    company.getCity(),
                    company.getStateProvince(),
                    company.getCountry(),
                    company.getPostalCode(),
                    company.getContactPerson(),
                    company.getBusinessEmail(),
                    company.getPhoneNumber(),
                    company.getTimezone(),
                    company.getMeasurementUnits(),
                    company.getCargoTemperaturePolicy(),
                    company.getLogoReference(),
                    company.getDataRetentionDays(),
                    company.getCreatedAt(),
                    company.getUpdatedAt()
            );
        }
    }
}
