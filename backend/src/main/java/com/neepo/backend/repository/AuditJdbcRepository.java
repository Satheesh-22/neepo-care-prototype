package com.neepo.backend.repository;

import com.neepo.backend.dto.AuditEventResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditJdbcRepository {

    private static final String INSERT_AUDIT_SQL = """
            INSERT INTO audit_events (
                id, trace_id, actor_user_id, event_type, aggregate_type, aggregate_id, occurred_at, details, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
            """;

    private static final String FIND_BY_AGGREGATE_SQL = """
            SELECT id, trace_id, actor_user_id, event_type, aggregate_type, aggregate_id, occurred_at, details::text as details_text, created_at
            FROM audit_events
            WHERE aggregate_type = ? AND aggregate_id = ?
            ORDER BY occurred_at DESC, created_at DESC
            LIMIT ?
            """;

    private static final String FIND_BY_TYPE_SQL = """
            SELECT id, trace_id, actor_user_id, event_type, aggregate_type, aggregate_id, occurred_at, details::text as details_text, created_at
            FROM audit_events
            WHERE aggregate_type = ?
            ORDER BY occurred_at DESC, created_at DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void record(UUID traceId, UUID actorUserId, String eventType, String aggregateType, UUID aggregateId, Instant occurredAt, Object details) {
        String jsonDetails = json(details);
        Instant now = Instant.now();
        Instant eventTime = occurredAt != null ? occurredAt : now;
        jdbc.update(INSERT_AUDIT_SQL,
                UUID.randomUUID(),
                traceId,
                actorUserId,
                eventType,
                aggregateType,
                aggregateId,
                Timestamp.from(eventTime),
                jsonDetails,
                Timestamp.from(now));
    }

    public List<AuditEventResponse> listByAggregate(String aggregateType, UUID aggregateId, int limit) {
        return jdbc.query(FIND_BY_AGGREGATE_SQL,
                (rs, rowNum) -> new AuditEventResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("trace_id", UUID.class),
                        rs.getObject("actor_user_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("details_text"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                aggregateType, aggregateId, limit > 0 ? limit : 50);
    }

    public List<AuditEventResponse> listByAggregateType(String aggregateType, int limit) {
        return jdbc.query(FIND_BY_TYPE_SQL,
                (rs, rowNum) -> new AuditEventResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("trace_id", UUID.class),
                        rs.getObject("actor_user_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("details_text"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                aggregateType, limit > 0 ? limit : 50);
    }

    private String json(Object value) {
        if (value == null) {
            return "{}";
        }
        if (value instanceof String s) {
            return s.isBlank() ? "{}" : s;
        }
        try {
            return objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(value);
        } catch (JacksonException exception) {
            return "{}";
        }
    }
}
