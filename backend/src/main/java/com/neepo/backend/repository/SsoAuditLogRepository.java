package com.neepo.backend.repository;

import com.neepo.backend.entity.SsoAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SsoAuditLogRepository extends JpaRepository<SsoAuditLog, UUID> {
    List<SsoAuditLog> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<SsoAuditLog> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);
}
