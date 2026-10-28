package com.neepo.backend.dto;

public record ComplianceExportPackageResponse(
        ComplianceExportManifestResponse manifest,
        String rawPayload,
        String mediaType,
        String filename
) {
}
