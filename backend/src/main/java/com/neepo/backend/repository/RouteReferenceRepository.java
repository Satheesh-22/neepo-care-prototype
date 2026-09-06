package com.neepo.backend.repository;

import com.neepo.backend.entity.RouteReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RouteReferenceRepository extends JpaRepository<RouteReference, UUID> {
    boolean existsByIdAndCompanyId(UUID id, UUID companyId);
}
