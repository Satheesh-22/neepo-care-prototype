package com.neepo.backend.repository;

import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RecoveryQueryJdbcRepository {
    private static final String FIND_PLAN_SQL = """
            SELECT p.id, p.trip_id, p.disruption_id, p.plan_type, p.status,
                   p.provider_id, p.safe_stop_id, p.generated_at, p.selected_at,
                   e.feasible, e.violated_hard_constraints, e.warnings,
                   e.soft_constraint_results::text AS soft_constraint_results,
                   e.risk_score, e.uncertainty_score, e.benefit_score,
                   e.lambda_coefficient, e.mu_coefficient, e.final_utility,
                   e.confidence, e.freshness_summary::text AS freshness_summary,
                   e.explanation_codes, e.evaluated_at, e.evaluator_version, e.input_hash
            FROM recovery_plans p
            JOIN plan_evaluations e ON e.plan_id = p.id
            JOIN trips t ON t.id = p.trip_id
            JOIN disruption_events d ON d.id = p.disruption_id
            WHERE p.id = ? AND t.company_id = ? AND d.active = TRUE
            """;
    private static final String FIND_PLANS_SQL = """
            SELECT p.id, p.trip_id, p.disruption_id, p.plan_type, p.status,
                   p.provider_id, p.safe_stop_id, p.generated_at, p.selected_at,
                   e.feasible, e.violated_hard_constraints, e.warnings,
                   e.soft_constraint_results::text AS soft_constraint_results,
                   e.risk_score, e.uncertainty_score, e.benefit_score,
                   e.lambda_coefficient, e.mu_coefficient, e.final_utility,
                   e.confidence, e.freshness_summary::text AS freshness_summary,
                   e.explanation_codes, e.evaluated_at, e.evaluator_version, e.input_hash
            FROM recovery_plans p
            JOIN plan_evaluations e ON e.plan_id = p.id
            JOIN trips t ON t.id = p.trip_id
            WHERE p.trip_id = ? AND t.company_id = ?
              AND p.disruption_id = (
                  SELECT d.id
                  FROM disruption_events d
                  WHERE d.trip_id = p.trip_id AND d.active = TRUE
                  ORDER BY d.detected_at DESC, d.id
                  LIMIT 1
              )
            ORDER BY p.generation_sequence, p.id
            """;
    private static final String FIND_STEPS_SQL = """
            SELECT sequence_number, action_code, instruction, provider_id,
                   expected_start_at, expected_complete_at,
                   required_resources::text AS required_resources
            FROM recovery_plan_steps
            WHERE plan_id = ?
            ORDER BY sequence_number
            """;
    private static final String FIND_EXPLANATION_SQL = """
            SELECT a.details::text AS details
            FROM audit_events a
            JOIN disruption_events d ON d.id = a.aggregate_id
            JOIN trips t ON t.id = d.trip_id
            WHERE d.trip_id = ? AND t.company_id = ?
              AND a.event_type = 'RECOVERY_PLAN_SELECTED'
              AND a.aggregate_type = 'DISRUPTION'
            ORDER BY a.occurred_at DESC, a.created_at DESC, a.id
            LIMIT 1
            """;
    private static final TypeReference<Map<String, BigDecimal>> SCORE_MAP = new TypeReference<>() { };
    private static final TypeReference<Map<String, String>> FRESHNESS_MAP = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RecoveryQueryJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<RecoveryPlan> findPlans(UUID companyId, UUID tripId) {
        return jdbc.query(FIND_PLANS_SQL, (resultSet, rowNumber) -> mapPlan(resultSet), tripId, companyId);
    }

    public Optional<RecoveryPlan> findPlan(UUID companyId, UUID planId) {
        return jdbc.query(FIND_PLAN_SQL, (resultSet, rowNumber) -> mapPlan(resultSet), planId, companyId)
                .stream()
                .findFirst();
    }

    private RecoveryPlan mapPlan(ResultSet resultSet) throws SQLException {
        UUID planId = resultSet.getObject("id", UUID.class);
        PlanEvaluation evaluation = new PlanEvaluation(
                planId,
                resultSet.getBoolean("feasible"),
                strings(resultSet.getArray("violated_hard_constraints")),
                strings(resultSet.getArray("warnings")),
                json(resultSet.getString("soft_constraint_results"), SCORE_MAP),
                resultSet.getBigDecimal("risk_score"),
                resultSet.getBigDecimal("uncertainty_score"),
                resultSet.getBigDecimal("benefit_score"),
                resultSet.getBigDecimal("lambda_coefficient"),
                resultSet.getBigDecimal("mu_coefficient"),
                resultSet.getBigDecimal("final_utility"),
                resultSet.getBigDecimal("confidence").doubleValue(),
                json(resultSet.getString("freshness_summary"), FRESHNESS_MAP),
                strings(resultSet.getArray("explanation_codes")),
                resultSet.getObject("evaluated_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("evaluator_version"),
                resultSet.getString("input_hash")
        );
        return new RecoveryPlan(
                planId,
                resultSet.getObject("trip_id", UUID.class),
                resultSet.getObject("disruption_id", UUID.class),
                RecoveryPlanType.valueOf(resultSet.getString("plan_type")),
                RecoveryPlanStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("provider_id", UUID.class),
                resultSet.getObject("safe_stop_id", UUID.class),
                findSteps(planId),
                evaluation,
                resultSet.getObject("generated_at", OffsetDateTime.class).toInstant(),
                instant(resultSet.getObject("selected_at", OffsetDateTime.class))
        );
    }

    public Optional<DecisionExplanation> findLatestExplanation(UUID companyId, UUID tripId) {
        return jdbc.query(FIND_EXPLANATION_SQL, (resultSet, rowNumber) -> {
            try {
                return objectMapper.readValue(resultSet.getString("details"), DecisionExplanation.class);
            } catch (JacksonException exception) {
                throw new SQLException("Could not deserialize decision explanation", exception);
            }
        }, tripId, companyId).stream().findFirst();
    }

    private List<RecoveryPlanStep> findSteps(UUID planId) {
        return jdbc.query(FIND_STEPS_SQL, (resultSet, rowNumber) -> new RecoveryPlanStep(
                resultSet.getInt("sequence_number"),
                resultSet.getString("action_code"),
                resultSet.getString("instruction"),
                resultSet.getObject("provider_id", UUID.class),
                resultSet.getObject("expected_start_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("expected_complete_at", OffsetDateTime.class).toInstant(),
                json(resultSet.getString("required_resources"), STRING_LIST)
        ), planId);
    }

    private <T> T json(String value, TypeReference<T> type) throws SQLException {
        try {
            return objectMapper.readValue(value, type);
        } catch (JacksonException exception) {
            throw new SQLException("Could not deserialize recovery decision data", exception);
        }
    }

    private static List<String> strings(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        try {
            Object values = array.getArray();
            return values instanceof Object[] entries
                    ? Arrays.stream(entries).map(Object::toString).toList()
                    : List.of();
        } finally {
            array.free();
        }
    }

    private static java.time.Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
