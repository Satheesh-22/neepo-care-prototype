package com.neepo.backend.repository;

import com.neepo.backend.entity.GatewaySyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GatewaySyncLogRepository extends JpaRepository<GatewaySyncLog, UUID> {
    List<GatewaySyncLog> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<GatewaySyncLog> findAllByGatewayIdOrderByCreatedAtDesc(UUID gatewayId);
}
