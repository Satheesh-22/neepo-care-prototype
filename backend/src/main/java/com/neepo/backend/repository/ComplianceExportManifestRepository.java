package com.neepo.backend.repository;

import com.neepo.backend.entity.ComplianceExportManifest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceExportManifestRepository extends JpaRepository<ComplianceExportManifest, UUID> {

    List<ComplianceExportManifest> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    Optional<ComplianceExportManifest> findByIdAndCompanyId(UUID id, UUID companyId);

    List<ComplianceExportManifest> findByTripIdAndCompanyIdOrderByCreatedAtDesc(UUID tripId, UUID companyId);

    Optional<ComplianceExportManifest> findBySha256Digest(String sha256Digest);
}
