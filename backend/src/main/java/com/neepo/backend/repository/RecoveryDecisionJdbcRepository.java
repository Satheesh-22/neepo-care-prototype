package com.neepo.backend.repository;

import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStep;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class RecoveryDecisionJdbcRepository {
    private static final String LOCK_SCOPE_SQL = """
            SELECT d.id
            FROM disruption_events d
            JOIN trips t ON t.id = d.trip_id
            JOIN trip_context_snapshots context
              ON context.id = ? AND context.trip_id = d.trip_id
            WHERE d.id = ?
              AND d.trip_id = ?
              AND d.active = TRUE
              AND t.company_id = ?
            FOR UPDATE OF d
            """;
    private static final String NEXT_GENERATION_SEQUENCE_SQL = """
            SELECT COALESCE(MAX(generation_sequence), 0) + 1
            FROM recovery_plans
            WHERE disruption_id = ?
            """;
    private static final String INSERT_PLAN_SQL = """
            INSERT INTO recovery_plans (
                id, trip_id, disruption_id, plan_type, status, provider_id, safe_stop_id,
                generated_at, selected_at, generation_sequence
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_STEP_SQL = """
            INSERT INTO recovery_plan_steps (
                plan_id, sequence_number, action_code, instruction, provider_id,
                expected_start_at, expected_complete_at, required_resources
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            """;
    private static final String INSERT_EVALUATION_SQL = """
            INSERT INTO plan_evaluations (
                plan_id, feasible, violated_hard_constraints, warnings, soft_constraint_results,
                risk_score, uncertainty_score, benefit_score, lambda_coefficient, mu_coefficient,
                final_utility, confidence, freshness_summary, explanation_codes, evaluated_at,
                evaluator_version, input_hash
            ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            """;
    private static final String INSERT_EXPLANATION_AUDIT_SQL = """
            INSERT INTO audit_events (
                trace_id, event_type, aggregate_type, aggregate_id, occurred_at, details
            ) VALUES (?, 'RECOVERY_PLAN_SELECTED', 'DISRUPTION', ?, ?, ?::jsonb)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RecoveryDecisionJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public boolean lockScope(UUID companyId, UUID tripId, UUID contextSnapshotId, UUID disruptionId) {
        return !jdbc.query(
                LOCK_SCOPE_SQL,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                contextSnapshotId,
                disruptionId,
                tripId,
                companyId
        ).isEmpty();
    }

    public int nextGenerationSequence(UUID disruptionId) {
        Integer sequence = jdbc.queryForObject(NEXT_GENERATION_SEQUENCE_SQL, Integer.class, disruptionId);
        if (sequence == null || sequence <= 0) {
            throw new IllegalStateException("Could not allocate a recovery-plan generation sequence");
        }
        return sequence;
    }

    public void insertPlan(RecoveryPlan plan, int generationSequence) {
        requireSingleInsert(jdbc.update(
                INSERT_PLAN_SQL,
                plan.id(),
                plan.tripId(),
                plan.disruptionId(),
                plan.type().name(),
                plan.status().name(),
                plan.providerId(),
                plan.safeStopId(),
                timestamp(plan.generatedAt()),
                timestamp(plan.selectedAt()),
                generationSequence
        ), "Recovery plan");

        for (RecoveryPlanStep step : plan.steps()) {
            requireSingleInsert(jdbc.update(
                    INSERT_STEP_SQL,
                    plan.id(),
                    step.sequence(),
                    step.actionCode(),
                    step.instruction(),
                    step.providerId(),
                    timestamp(step.expectedStartAt()),
                    timestamp(step.expectedCompleteAt()),
                    json(step.requiredResources())
            ), "Recovery plan step");
        }
        insertEvaluation(plan.evaluation());
    }

    public void insertExplanationAudit(UUID traceId, DecisionExplanation explanation) {
        requireSingleInsert(jdbc.update(
                INSERT_EXPLANATION_AUDIT_SQL,
                traceId,
                explanation.disruptionId(),
                timestamp(explanation.explainedAt()),
                json(explanation)
        ), "Decision explanation audit event");
    }

    private void insertEvaluation(PlanEvaluation evaluation) {
        int inserted = jdbc.update(connection -> {
            var statement = connection.prepareStatement(INSERT_EVALUATION_SQL);
            statement.setObject(1, evaluation.planId());
            statement.setBoolean(2, evaluation.feasible());
            statement.setArray(3, connection.createArrayOf(
                    "text", evaluation.violatedHardConstraints().toArray(String[]::new)
            ));
            statement.setArray(4, connection.createArrayOf(
                    "text", evaluation.warnings().toArray(String[]::new)
            ));
            statement.setString(5, json(evaluation.softConstraintResults()));
            statement.setBigDecimal(6, evaluation.riskScore());
            statement.setBigDecimal(7, evaluation.uncertaintyScore());
            statement.setBigDecimal(8, evaluation.benefitScore());
            statement.setBigDecimal(9, evaluation.lambdaCoefficient());
            statement.setBigDecimal(10, evaluation.muCoefficient());
            statement.setBigDecimal(11, evaluation.finalUtility());
            statement.setBigDecimal(12, java.math.BigDecimal.valueOf(evaluation.confidence()));
            statement.setString(13, json(evaluation.freshnessSummary()));
            statement.setArray(14, connection.createArrayOf(
                    "text", evaluation.explanationCodes().toArray(String[]::new)
            ));
            statement.setTimestamp(15, Timestamp.from(evaluation.evaluatedAt()));
            statement.setString(16, evaluation.evaluatorVersion());
            statement.setString(17, evaluation.inputHash());
            return statement;
        });
        requireSingleInsert(inserted, "Plan evaluation");
    }

    private String json(Object value) {
        try {
            return objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Recovery decision cannot be serialized", exception);
        }
    }

    private static void requireSingleInsert(int inserted, String recordType) {
        if (inserted != 1) {
            throw new IllegalStateException(recordType + " was not persisted");
        }
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
