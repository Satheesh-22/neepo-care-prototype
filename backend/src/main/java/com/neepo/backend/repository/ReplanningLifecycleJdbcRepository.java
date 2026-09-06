package com.neepo.backend.repository;

import com.neepo.backend.dto.RecoveryPlanLifecycleScope;
import com.neepo.backend.dto.ReplanningCycleRecord;
import com.neepo.contracts.PlanInvalidation;
import com.neepo.contracts.PlanInvalidationReason;
import com.neepo.contracts.RecoveryPlanStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReplanningLifecycleJdbcRepository {
    private static final String LOCK_PLAN_SQL = """
            SELECT p.id, p.trip_id, p.disruption_id, p.status
            FROM recovery_plans p
            JOIN trips t ON t.id = p.trip_id
            JOIN disruption_events d ON d.id = p.disruption_id
            WHERE p.id = ? AND t.company_id = ? AND d.active = TRUE
            FOR UPDATE OF p, d
            """;
    private static final String FIND_INVALIDATION_SQL = """
            SELECT id, event_id, plan_id, reason, explanation_code, observed_at, details
            FROM plan_invalidations
            WHERE event_id = ?
            """;
    private static final String INSERT_INVALIDATION_SQL = """
            INSERT INTO plan_invalidations (
                id, event_id, plan_id, reason, explanation_code, observed_at, details
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INVALIDATE_PLAN_SQL = """
            UPDATE recovery_plans
            SET status = 'INVALIDATED', updated_at = CURRENT_TIMESTAMP, version = version + 1
            WHERE id = ? AND status IN ('SELECTED', 'EXECUTING')
            """;
    private static final String NEXT_CYCLE_SQL = """
            SELECT COALESCE(MAX(cycle_number), 0) + 1
            FROM replanning_cycles
            WHERE disruption_id = ?
            """;
    private static final String INSERT_CYCLE_SQL = """
            INSERT INTO replanning_cycles (
                id, trip_id, disruption_id, invalidation_id, cycle_number, status, started_at
            ) VALUES (?, ?, ?, ?, ?, 'STARTED', ?)
            """;
    private static final String FIND_CYCLE_BY_INVALIDATION_SQL = """
            SELECT id, trip_id, disruption_id, invalidation_id, cycle_number, status,
                   started_at, completed_at, selected_plan_id
            FROM replanning_cycles
            WHERE invalidation_id = ?
            """;
    private static final String LOCK_CYCLE_SQL = """
            SELECT cycle.id, cycle.trip_id, cycle.disruption_id, cycle.invalidation_id,
                   cycle.cycle_number, cycle.status, cycle.started_at,
                   cycle.completed_at, cycle.selected_plan_id
            FROM replanning_cycles cycle
            JOIN trips t ON t.id = cycle.trip_id
            WHERE cycle.id = ? AND t.company_id = ?
            FOR UPDATE OF cycle
            """;
    private static final String COMPLETE_CYCLE_SQL = """
            UPDATE replanning_cycles
            SET status = 'COMPLETED', completed_at = ?, selected_plan_id = ?
            WHERE id = ? AND status = 'STARTED'
            """;
    private static final String FAIL_CYCLE_SQL = """
            UPDATE replanning_cycles
            SET status = 'FAILED', completed_at = ?
            WHERE id = ? AND status = 'STARTED'
            """;
    private static final String INSERT_AUDIT_SQL = """
            INSERT INTO audit_events (
                trace_id, event_type, aggregate_type, aggregate_id, occurred_at, details
            ) VALUES (?, ?, ?, ?, ?, ?::jsonb)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ReplanningLifecycleJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<RecoveryPlanLifecycleScope> lockPlan(UUID companyId, UUID planId) {
        return jdbc.query(LOCK_PLAN_SQL, (resultSet, rowNumber) -> new RecoveryPlanLifecycleScope(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("trip_id", UUID.class),
                resultSet.getObject("disruption_id", UUID.class),
                RecoveryPlanStatus.valueOf(resultSet.getString("status"))
        ), planId, companyId).stream().findFirst();
    }

    public Optional<PlanInvalidation> findInvalidation(UUID eventId) {
        return jdbc.query(FIND_INVALIDATION_SQL, (resultSet, rowNumber) -> new PlanInvalidation(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("event_id", UUID.class),
                resultSet.getObject("plan_id", UUID.class),
                PlanInvalidationReason.valueOf(resultSet.getString("reason")),
                resultSet.getString("explanation_code"),
                resultSet.getObject("observed_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("details")
        ), eventId).stream().findFirst();
    }

    public void insertInvalidation(PlanInvalidation invalidation) {
        requireOne(jdbc.update(
                INSERT_INVALIDATION_SQL,
                invalidation.id(), invalidation.eventId(), invalidation.planId(),
                invalidation.reason().name(), invalidation.explanationCode(),
                Timestamp.from(invalidation.observedAt()), invalidation.details()
        ), "Plan invalidation");
    }

    public void invalidatePlan(UUID planId) {
        requireOne(jdbc.update(INVALIDATE_PLAN_SQL, planId), "Recovery plan invalidation state");
    }

    public int nextCycleNumber(UUID disruptionId) {
        Integer cycle = jdbc.queryForObject(NEXT_CYCLE_SQL, Integer.class, disruptionId);
        if (cycle == null || cycle <= 0) {
            throw new IllegalStateException("Could not allocate a replanning cycle number");
        }
        return cycle;
    }

    public ReplanningCycleRecord insertStartedCycle(
            UUID id, UUID tripId, UUID disruptionId, UUID invalidationId, int number, Instant startedAt
    ) {
        requireOne(jdbc.update(
                INSERT_CYCLE_SQL, id, tripId, disruptionId, invalidationId, number, Timestamp.from(startedAt)
        ), "Replanning cycle");
        return new ReplanningCycleRecord(
                id, tripId, disruptionId, invalidationId, number, "STARTED", startedAt, null, null
        );
    }

    public Optional<ReplanningCycleRecord> findCycleByInvalidation(UUID invalidationId) {
        return queryCycle(FIND_CYCLE_BY_INVALIDATION_SQL, invalidationId);
    }

    public Optional<ReplanningCycleRecord> lockCycle(UUID companyId, UUID cycleId) {
        return queryCycle(LOCK_CYCLE_SQL, cycleId, companyId);
    }

    public void completeCycle(UUID cycleId, Instant completedAt, UUID selectedPlanId) {
        requireOne(jdbc.update(COMPLETE_CYCLE_SQL, Timestamp.from(completedAt), selectedPlanId, cycleId), "Replanning completion");
    }

    public void failCycle(UUID cycleId, Instant failedAt) {
        requireOne(jdbc.update(FAIL_CYCLE_SQL, Timestamp.from(failedAt), cycleId), "Replanning failure");
    }

    public void insertAudit(
            UUID traceId, String eventType, String aggregateType, UUID aggregateId,
            Instant occurredAt, Object details
    ) {
        requireOne(jdbc.update(
                INSERT_AUDIT_SQL,
                traceId, eventType, aggregateType, aggregateId, Timestamp.from(occurredAt), json(details)
        ), "Replanning audit event");
    }

    private Optional<ReplanningCycleRecord> queryCycle(String sql, Object... arguments) {
        return jdbc.query(sql, (resultSet, rowNumber) -> new ReplanningCycleRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("trip_id", UUID.class),
                resultSet.getObject("disruption_id", UUID.class),
                resultSet.getObject("invalidation_id", UUID.class),
                resultSet.getInt("cycle_number"),
                resultSet.getString("status"),
                resultSet.getObject("started_at", OffsetDateTime.class).toInstant(),
                instant(resultSet.getObject("completed_at", OffsetDateTime.class)),
                resultSet.getObject("selected_plan_id", UUID.class)
        ), arguments).stream().findFirst();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Replanning lifecycle details cannot be serialized", exception);
        }
    }

    private static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private static void requireOne(int changed, String recordType) {
        if (changed != 1) {
            throw new IllegalStateException(recordType + " was not persisted");
        }
    }
}
