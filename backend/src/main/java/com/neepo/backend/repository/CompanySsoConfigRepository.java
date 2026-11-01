package com.neepo.backend.repository;

import com.neepo.backend.entity.CompanySsoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanySsoConfigRepository extends JpaRepository<CompanySsoConfig, UUID> {
    Optional<CompanySsoConfig> findByCompanyId(UUID companyId);
    Optional<CompanySsoConfig> findBySsoDomainIgnoreCase(String ssoDomain);
    boolean existsBySsoDomainIgnoreCaseAndCompanyIdNot(String ssoDomain, UUID companyId);
}
