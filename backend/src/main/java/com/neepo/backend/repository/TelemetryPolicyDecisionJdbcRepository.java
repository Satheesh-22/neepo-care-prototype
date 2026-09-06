package com.neepo.backend.repository;

import com.neepo.care.telemetry.TelemetryPolicyDecision;
import com.neepo.contracts.TelemetryPolicyState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TelemetryPolicyDecisionJdbcRepository {
    private static final String FIND_SQL = """
            SELECT trip_id, event_id, policy_state, interval_seconds, decided_at, explanation_code
            FROM telemetry_policy_decisions
            WHERE event_id = ?
            """;
    private static final String INSERT_SQL = """
            INSERT INTO telemetry_policy_decisions (
                trip_id, event_id, policy_state, interval_seconds, decided_at, explanation_code
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;
    private static final String AUDIT_SQL = """
            INSERT INTO audit_events (
                trace_id, event_type, aggregate_type, aggregate_id, occurred_at, details
            ) VALUES (?, 'TELEMETRY_POLICY_DECIDED', 'TRIP', ?, ?, ?::jsonb)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public TelemetryPolicyDecisionJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<TelemetryPolicyDecision> findByEventId(UUID eventId) {
        return jdbc.query(FIND_SQL, (resultSet, rowNumber) -> new TelemetryPolicyDecision(
                resultSet.getObject("trip_id", UUID.class),
                resultSet.getObject("event_id", UUID.class),
                TelemetryPolicyState.valueOf(resultSet.getString("policy_state")),
                (Integer) resultSet.getObject("interval_seconds"),
                resultSet.getObject("decided_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("explanation_code")
        ), eventId).stream().findFirst();
    }

    public boolean insertIfAbsent(TelemetryPolicyDecision decision) {
        return jdbc.update(
                INSERT_SQL,
                decision.tripId(), decision.eventId(), decision.policyState().name(),
                decision.intervalSeconds(), Timestamp.from(decision.decidedAt()), decision.explanationCode()
        ) == 1;
    }

    public void insertAudit(UUID traceId, TelemetryPolicyDecision decision) {
        int inserted = jdbc.update(
                AUDIT_SQL,
                traceId, decision.tripId(), Timestamp.from(decision.decidedAt()), json(decision)
        );
        if (inserted != 1) {
            throw new IllegalStateException("Telemetry policy audit event was not persisted");
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Telemetry policy decision cannot be serialized", exception);
        }
    }
}
