package com.neepo.backend.entity;

import com.neepo.backend.domain.ComplianceExportFormat;
import com.neepo.backend.domain.ComplianceExportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_export_manifests")
public class ComplianceExportManifest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 50)
    private ComplianceExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplianceExportFormat format;

    @Column(name = "sha256_digest", nullable = false, length = 64)
    private String sha256Digest;

    @Column(name = "record_count", nullable = false)
    private int recordCount;

    @Column(name = "generated_by_user_id")
    private UUID generatedByUserId;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ComplianceExportManifest() {
    }

    public static ComplianceExportManifest create(
            UUID companyId,
            UUID tripId,
            ComplianceExportType exportType,
            ComplianceExportFormat format,
            String sha256Digest,
            int recordCount,
            UUID generatedByUserId,
            String metadataJson) {
        ComplianceExportManifest manifest = new ComplianceExportManifest();
        manifest.companyId = companyId;
        manifest.tripId = tripId;
        manifest.exportType = exportType;
        manifest.format = format;
        manifest.sha256Digest = sha256Digest;
        manifest.recordCount = recordCount;
        manifest.generatedByUserId = generatedByUserId;
        manifest.metadata = (metadataJson != null && !metadataJson.isBlank()) ? metadataJson : "{}";
        return manifest;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (metadata == null || metadata.isBlank()) {
            metadata = "{}";
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public ComplianceExportType getExportType() {
        return exportType;
    }

    public ComplianceExportFormat getFormat() {
        return format;
    }

    public String getSha256Digest() {
        return sha256Digest;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public UUID getGeneratedByUserId() {
        return generatedByUserId;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
