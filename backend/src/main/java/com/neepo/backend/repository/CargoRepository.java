package com.neepo.backend.repository;

import com.neepo.backend.entity.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CargoRepository extends JpaRepository<Cargo, UUID> {
    Optional<Cargo> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndReferenceCodeIgnoreCase(UUID companyId, String referenceCode);
}
