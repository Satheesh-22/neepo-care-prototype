package com.neepo.backend.repository;

import com.neepo.backend.entity.FleetAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FleetAnalyticsRepository extends JpaRepository<FleetAnalyticsSnapshot, UUID> {

    Optional<FleetAnalyticsSnapshot> findFirstByCompanyIdAndTimeframeOrderBySnapshotAtDesc(UUID companyId, String timeframe);

    List<FleetAnalyticsSnapshot> findByCompanyIdOrderBySnapshotAtDesc(UUID companyId);
}
