package com.neepo.backend.repository;

import com.neepo.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    Optional<Trip> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndReferenceCodeIgnoreCase(UUID companyId, String referenceCode);
}
