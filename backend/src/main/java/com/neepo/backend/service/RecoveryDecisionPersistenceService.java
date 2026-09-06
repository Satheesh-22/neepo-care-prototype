package com.neepo.backend.service;

import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.RecoveryDecisionJdbcRepository;
import com.neepo.care.orchestration.RecoveryDecisionResult;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecoveryDecisionPersistenceService {
    private final RecoveryDecisionJdbcRepository decisions;

    public RecoveryDecisionPersistenceService(RecoveryDecisionJdbcRepository decisions) {
        this.decisions = Objects.requireNonNull(decisions, "Recovery decision repository is required");
    }

    @Transactional
    public RecoveryPlan persist(
            UUID companyId,
            UUID contextSnapshotId,
            UUID disruptionId,
            UUID traceId,
            RecoveryDecisionResult decision
    ) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(contextSnapshotId, "Context snapshot id is required");
        Objects.requireNonNull(disruptionId, "Disruption id is required");
        Objects.requireNonNull(traceId, "Trace id is required");
        Objects.requireNonNull(decision, "Recovery decision is required");

        List<RecoveryPlan> plans = decision.selection().plans();
        RecoveryPlan selected = decision.selection().selectedPlan();
        DecisionExplanation explanation = decision.selection().explanation();
        validate(disruptionId, plans, selected, explanation);

        UUID tripId = selected.tripId();
        if (!decisions.lockScope(companyId, tripId, contextSnapshotId, disruptionId)) {
            throw new ResourceNotFoundException(
                    "RECOVERY_DECISION_SCOPE_NOT_FOUND",
                    "Active disruption and context snapshot were not found for this tenant trip"
            );
        }

        int sequence = decisions.nextGenerationSequence(disruptionId);
        for (RecoveryPlan plan : plans) {
            decisions.insertPlan(plan, sequence++);
        }
        decisions.insertExplanationAudit(traceId, explanation);
        return selected;
    }

    private static void validate(
            UUID disruptionId,
            List<RecoveryPlan> plans,
            RecoveryPlan selected,
            DecisionExplanation explanation
    ) {
        Objects.requireNonNull(plans, "Recovery decision plans are required");
        Objects.requireNonNull(selected, "Selected recovery plan is required");
        Objects.requireNonNull(explanation, "Decision explanation is required");
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("Recovery decision must contain at least one plan");
        }

        UUID tripId = Objects.requireNonNull(selected.tripId(), "Selected plan trip id is required");
        if (!disruptionId.equals(selected.disruptionId())
                || selected.status() != RecoveryPlanStatus.SELECTED
                || selected.selectedAt() == null) {
            throw new IllegalArgumentException("Selected plan does not match the persisted disruption lifecycle");
        }

        Set<UUID> ids = new HashSet<>();
        Set<UUID> rejectedIds = new HashSet<>();
        long selectedCount = 0;
        for (RecoveryPlan plan : plans) {
            Objects.requireNonNull(plan, "Recovery plan entry is required");
            Objects.requireNonNull(plan.id(), "Recovery plan id is required");
            Objects.requireNonNull(plan.evaluation(), "Recovery plan evaluation is required");
            Objects.requireNonNull(plan.steps(), "Recovery plan steps are required");
            if (!ids.add(plan.id())) {
                throw new IllegalArgumentException("Recovery decision contains duplicate plan ids");
            }
            if (!tripId.equals(plan.tripId()) || !disruptionId.equals(plan.disruptionId())) {
                throw new IllegalArgumentException("Recovery plans must share the persisted trip and disruption");
            }
            if (!plan.id().equals(plan.evaluation().planId())) {
                throw new IllegalArgumentException("Recovery plan evaluation does not match its plan");
            }
            if (plan.status() == RecoveryPlanStatus.SELECTED) {
                selectedCount++;
                if (!plan.evaluation().feasible() || plan.selectedAt() == null) {
                    throw new IllegalArgumentException("Selected plan must be feasible and timestamped");
                }
            } else if (plan.status() == RecoveryPlanStatus.FEASIBLE) {
                if (!plan.evaluation().feasible() || plan.selectedAt() != null) {
                    throw new IllegalArgumentException("Feasible unselected plan has an invalid lifecycle state");
                }
            } else if (plan.status() == RecoveryPlanStatus.REJECTED) {
                if (plan.evaluation().feasible() || plan.selectedAt() != null) {
                    throw new IllegalArgumentException("Rejected plan has an invalid evaluation state");
                }
                rejectedIds.add(plan.id());
            } else {
                throw new IllegalArgumentException("Recovery decision contains a non-terminal evaluation status");
            }
        }
        RecoveryPlan persistedSelected = plans.stream()
                .filter(plan -> plan.id().equals(selected.id()))
                .findFirst()
                .orElse(null);
        if (selectedCount != 1 || !selected.equals(persistedSelected)) {
            throw new IllegalArgumentException("Recovery decision must contain exactly its selected plan");
        }
        if (!tripId.equals(explanation.tripId())
                || !disruptionId.equals(explanation.disruptionId())
                || !selected.id().equals(explanation.selectedPlanId())
                || !selected.selectedAt().equals(explanation.explainedAt())) {
            throw new IllegalArgumentException("Decision explanation does not match the selected plan");
        }
        if (!rejectedIds.equals(new HashSet<>(explanation.rejectedPlanIds()))
                || !rejectedIds.equals(explanation.rejectionExplanationCodes().keySet())) {
            throw new IllegalArgumentException("Decision explanation does not match the rejected plans");
        }
    }
}
