package com.neepo.backend.service;

import com.neepo.backend.dto.RecoveryPlanLifecycleScope;
import com.neepo.backend.dto.ReplanningCycleRecord;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.ReplanningLifecycleJdbcRepository;
import com.neepo.care.invalidation.RecoveryPlanInvalidationResult;
import com.neepo.care.orchestration.RecoveryReplanningResult;
import com.neepo.contracts.PlanInvalidation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReplanningLifecycleService {
    private final ReplanningLifecycleJdbcRepository lifecycles;
    private final RecoveryDecisionPersistenceService decisionPersistence;

    public ReplanningLifecycleService(
            ReplanningLifecycleJdbcRepository lifecycles,
            RecoveryDecisionPersistenceService decisionPersistence
    ) {
        this.lifecycles = Objects.requireNonNull(lifecycles, "Replanning lifecycle repository is required");
        this.decisionPersistence = Objects.requireNonNull(
                decisionPersistence, "Recovery decision persistence service is required"
        );
    }

    @Transactional
    public ReplanningCycleRecord start(
            UUID companyId,
            UUID traceId,
            RecoveryPlanInvalidationResult result
    ) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(traceId, "Trace id is required");
        Objects.requireNonNull(result, "Recovery plan invalidation result is required");
        PlanInvalidation invalidation = result.invalidation();
        RecoveryPlan invalidatedPlan = result.invalidatedPlan();
        validateInvalidationResult(result);

        RecoveryPlanLifecycleScope scope = lifecycles.lockPlan(companyId, invalidatedPlan.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RECOVERY_PLAN_NOT_FOUND", "Active recovery plan was not found for this tenant"
                ));
        validateScope(scope, invalidatedPlan);

        var existing = lifecycles.findInvalidation(invalidation.eventId());
        if (scope.status() == RecoveryPlanStatus.INVALIDATED) {
            if (existing.isPresent() && existing.get().equals(invalidation)) {
                return lifecycles.findCycleByInvalidation(invalidation.id())
                        .orElseThrow(ReplanningLifecycleService::incompleteLifecycle);
            }
            throw conflict("PLAN_ALREADY_INVALIDATED", "Recovery plan was invalidated by another event");
        }
        if (scope.status() != RecoveryPlanStatus.SELECTED
                && scope.status() != RecoveryPlanStatus.EXECUTING) {
            throw conflict("PLAN_NOT_ACTIVE", "Only selected or executing plans can be invalidated");
        }
        if (existing.isPresent()) {
            throw conflict("INVALIDATION_EVENT_REUSED", "Invalidation event id has already been used");
        }

        lifecycles.insertInvalidation(invalidation);
        lifecycles.invalidatePlan(invalidatedPlan.id());
        lifecycles.insertAudit(
                traceId, "PLAN_INVALIDATED", "RECOVERY_PLAN", invalidatedPlan.id(),
                invalidation.observedAt(), invalidation
        );

        int cycleNumber = lifecycles.nextCycleNumber(invalidatedPlan.disruptionId());
        ReplanningCycleRecord cycle = lifecycles.insertStartedCycle(
                UUID.randomUUID(),
                invalidatedPlan.tripId(),
                invalidatedPlan.disruptionId(),
                invalidation.id(),
                cycleNumber,
                result.replanningTrigger().requestedAt()
        );
        lifecycles.insertAudit(
                traceId, "REPLANNING_STARTED", "REPLANNING_CYCLE", cycle.id(),
                cycle.startedAt(), result.replanningTrigger()
        );
        return cycle;
    }

    @Transactional
    public ReplanningCycleRecord complete(
            UUID companyId,
            UUID contextSnapshotId,
            UUID traceId,
            UUID cycleId,
            RecoveryReplanningResult result
    ) {
        Objects.requireNonNull(contextSnapshotId, "Context snapshot id is required");
        Objects.requireNonNull(result, "Recovery replanning result is required");
        ReplanningCycleRecord cycle = lockCycle(companyId, traceId, cycleId);
        RecoveryPlan selected = result.decision().selection().selectedPlan();
        validateCompletion(cycle, result, selected);

        if (cycle.status().equals("COMPLETED")) {
            if (selected.id().equals(cycle.selectedPlanId())) {
                return cycle;
            }
            throw conflict("REPLANNING_ALREADY_COMPLETED", "Cycle completed with another selected plan");
        }
        if (!cycle.status().equals("STARTED")) {
            throw conflict("REPLANNING_NOT_ACTIVE", "Only a started replanning cycle can complete");
        }

        Instant completedAt = result.decision().selection().explanation().explainedAt();
        if (completedAt.isBefore(cycle.startedAt())) {
            throw new IllegalArgumentException("Replanning completion cannot precede cycle start");
        }
        decisionPersistence.persist(
                companyId,
                contextSnapshotId,
                cycle.disruptionId(),
                traceId,
                result.decision()
        );
        lifecycles.completeCycle(cycle.id(), completedAt, selected.id());
        lifecycles.insertAudit(
                traceId, "REPLANNING_COMPLETED", "REPLANNING_CYCLE", cycle.id(),
                completedAt, result.decision().selection().explanation()
        );
        return new ReplanningCycleRecord(
                cycle.id(), cycle.tripId(), cycle.disruptionId(), cycle.invalidationId(),
                cycle.cycleNumber(), "COMPLETED", cycle.startedAt(), completedAt, selected.id()
        );
    }

    @Transactional
    public ReplanningCycleRecord fail(
            UUID companyId,
            UUID traceId,
            UUID cycleId,
            Instant failedAt,
            String reason
    ) {
        Objects.requireNonNull(failedAt, "Replanning failure time is required");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Replanning failure reason is required");
        }
        ReplanningCycleRecord cycle = lockCycle(companyId, traceId, cycleId);
        if (cycle.status().equals("FAILED")) {
            return cycle;
        }
        if (!cycle.status().equals("STARTED")) {
            throw conflict("REPLANNING_NOT_ACTIVE", "Only a started replanning cycle can fail");
        }
        if (failedAt.isBefore(cycle.startedAt())) {
            throw new IllegalArgumentException("Replanning failure cannot precede cycle start");
        }
        lifecycles.failCycle(cycle.id(), failedAt);
        lifecycles.insertAudit(
                traceId, "REPLANNING_FAILED", "REPLANNING_CYCLE", cycle.id(),
                failedAt, Map.of("reason", reason)
        );
        return new ReplanningCycleRecord(
                cycle.id(), cycle.tripId(), cycle.disruptionId(), cycle.invalidationId(),
                cycle.cycleNumber(), "FAILED", cycle.startedAt(), failedAt, null
        );
    }

    private ReplanningCycleRecord lockCycle(UUID companyId, UUID traceId, UUID cycleId) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(traceId, "Trace id is required");
        Objects.requireNonNull(cycleId, "Replanning cycle id is required");
        return lifecycles.lockCycle(companyId, cycleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "REPLANNING_CYCLE_NOT_FOUND", "Replanning cycle was not found for this tenant"
                ));
    }

    private static void validateInvalidationResult(RecoveryPlanInvalidationResult result) {
        RecoveryPlan plan = result.invalidatedPlan();
        PlanInvalidation invalidation = result.invalidation();
        var trigger = result.replanningTrigger();
        if (plan.status() != RecoveryPlanStatus.INVALIDATED
                || !plan.id().equals(invalidation.planId())
                || !plan.tripId().equals(trigger.tripId())
                || !plan.disruptionId().equals(trigger.disruptionId())
                || !invalidation.id().equals(trigger.invalidationId())
                || !invalidation.eventId().equals(trigger.sourceEventId())
                || !invalidation.observedAt().equals(trigger.requestedAt())) {
            throw new IllegalArgumentException("Invalidation result lifecycle references do not match");
        }
    }

    private static void validateScope(RecoveryPlanLifecycleScope scope, RecoveryPlan plan) {
        if (!scope.planId().equals(plan.id())
                || !scope.tripId().equals(plan.tripId())
                || !scope.disruptionId().equals(plan.disruptionId())) {
            throw new IllegalArgumentException("Invalidation result does not match the persisted plan scope");
        }
    }

    private static void validateCompletion(
            ReplanningCycleRecord cycle,
            RecoveryReplanningResult result,
            RecoveryPlan selected
    ) {
        if (!cycle.tripId().equals(result.trigger().tripId())
                || !cycle.disruptionId().equals(result.trigger().disruptionId())
                || !cycle.invalidationId().equals(result.trigger().invalidationId())
                || !cycle.tripId().equals(selected.tripId())
                || !cycle.disruptionId().equals(selected.disruptionId())) {
            throw new IllegalArgumentException("Replanning result does not match the persisted cycle");
        }
    }

    private static ResourceConflictException incompleteLifecycle() {
        return conflict(
                "REPLANNING_STATE_INCOMPLETE",
                "Invalidation was persisted without its replanning cycle"
        );
    }

    private static ResourceConflictException conflict(String code, String message) {
        return new ResourceConflictException(code, message);
    }
}
