package com.neepo.backend.service;

import com.neepo.backend.dto.ProviderAvailabilityRequest;
import com.neepo.backend.dto.ProviderResponse;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.ProviderJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProviderService {
    private final ProviderJdbcRepository providers;

    public ProviderService(ProviderJdbcRepository providers) {
        this.providers = providers;
    }

    public List<ProviderResponse> list(UUID companyId, int page, int size) {
        return providers.findAccessible(companyId, page, size);
    }

    @Transactional
    public ProviderResponse recordAvailability(
            UUID companyId,
            UUID providerId,
            ProviderAvailabilityRequest availability
    ) {
        if (!providers.appendAvailability(companyId, providerId, availability)) {
            throw providerNotFound();
        }
        return providers.findAccessibleById(companyId, providerId).orElseThrow(ProviderService::providerNotFound);
    }

    private static ResourceNotFoundException providerNotFound() {
        return new ResourceNotFoundException("PROVIDER_NOT_FOUND", "Provider was not found");
    }
}
