package com.neepo.backend.dto;

import com.neepo.backend.domain.ComplianceExportFormat;
import com.neepo.backend.domain.ComplianceExportType;
import com.neepo.backend.entity.ComplianceExportManifest;

import java.time.Instant;
import java.util.UUID;

public record ComplianceExportManifestResponse(
        UUID id,
        UUID companyId,
        UUID tripId,
        ComplianceExportType exportType,
        ComplianceExportFormat format,
        String sha256Digest,
        int recordCount,
        UUID generatedByUserId,
        String metadata,
        Instant createdAt
) {
    public static ComplianceExportManifestResponse from(ComplianceExportManifest manifest) {
        return new ComplianceExportManifestResponse(
                manifest.getId(),
                manifest.getCompanyId(),
                manifest.getTripId(),
                manifest.getExportType(),
                manifest.getFormat(),
                manifest.getSha256Digest(),
                manifest.getRecordCount(),
                manifest.getGeneratedByUserId(),
                manifest.getMetadata(),
                manifest.getCreatedAt()
        );
    }
}
