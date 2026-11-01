package com.neepo.backend.repository;

import com.neepo.backend.domain.DepotStatus;
import com.neepo.backend.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepotRepository extends JpaRepository<Depot, UUID> {

    List<Depot> findAllByCompanyIdOrderByCodeAsc(UUID companyId);

    Optional<Depot> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);

    boolean existsByCompanyIdAndCodeIgnoreCaseAndIdNot(UUID companyId, String code, UUID id);

    long countByCompanyId(UUID companyId);

    long countByCompanyIdAndStatus(UUID companyId, DepotStatus status);
}

