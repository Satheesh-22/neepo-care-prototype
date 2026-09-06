package com.neepo.backend.repository;

import com.neepo.backend.entity.TripAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TripAssignmentRepository extends JpaRepository<TripAssignment, UUID> {
    Optional<TripAssignment> findFirstByTripIdAndReleasedAtIsNull(UUID tripId);
}
