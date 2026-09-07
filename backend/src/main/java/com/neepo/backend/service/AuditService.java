package com.neepo.backend.service;

import com.neepo.backend.dto.AuditEventResponse;
import com.neepo.backend.repository.AuditJdbcRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditJdbcRepository auditRepository;

    public AuditService(AuditJdbcRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void record(UUID traceId, UUID actorUserId, String eventType, String aggregateType, UUID aggregateId, Map<String, Object> details) {
        UUID effectiveTraceId = traceId != null ? traceId : UUID.randomUUID();
        auditRepository.record(effectiveTraceId, actorUserId, eventType, aggregateType, aggregateId, Instant.now(), details);
    }

    public List<AuditEventResponse> getEventsForAggregate(String aggregateType, UUID aggregateId, int limit) {
        return auditRepository.listByAggregate(aggregateType, aggregateId, limit);
    }

    public List<AuditEventResponse> getEventsForAggregateType(String aggregateType, int limit) {
        return auditRepository.listByAggregateType(aggregateType, limit);
    }
}
