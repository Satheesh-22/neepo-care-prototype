package com.neepo.backend.service;

import com.neepo.backend.dto.InvalidateRecoveryPlanRequest;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.care.invalidation.ConfirmedPlanChange;
import com.neepo.care.invalidation.RecoveryPlanInvalidator;
import com.neepo.contracts.PlanInvalidationReason;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecoveryInvalidationService {
    private final RecoveryQueryJdbcRepository recovery;
    private final ReplanningLifecycleService replanning;
    private final RecoveryReplanningOrchestrationService replanningOrchestration;
    private final RecoveryPlanInvalidator invalidator;

    public RecoveryInvalidationService(
            RecoveryQueryJdbcRepository recovery,
            ReplanningLifecycleService replanning,
            RecoveryReplanningOrchestrationService replanningOrchestration
    ) {
        this.recovery = Objects.requireNonNull(recovery, "Recovery query repository is required");
        this.replanning = Objects.requireNonNull(replanning, "Replanning lifecycle service is required");
        this.replanningOrchestration = Objects.requireNonNull(
                replanningOrchestration, "Recovery replanning orchestration service is required"
        );
        this.invalidator = new RecoveryPlanInvalidator();
    }

    @Transactional
    public RecoveryPlan invalidate(
            UUID companyId,
            UUID traceId,
            UUID planId,
            InvalidateRecoveryPlanRequest request
    ) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(traceId, "Trace id is required");
        Objects.requireNonNull(planId, "Recovery plan id is required");
        Objects.requireNonNull(request, "Invalidation request is required");

        RecoveryPlan persisted = recovery.findPlan(companyId, planId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RECOVERY_PLAN_NOT_FOUND", "Active recovery plan was not found for this tenant"
                ));
        RecoveryPlan active = activeOrReplayable(persisted);
        PlanInvalidationReason reason = reason(request.explanationCode());
        var result = invalidator.invalidate(active, new ConfirmedPlanChange(
                request.eventId(), reason, request.observedAt(), request.details()
        ));

        var cycle = replanning.start(companyId, traceId, result);
        replanningOrchestration.complete(companyId, traceId, result, cycle);
        return result.invalidatedPlan();
    }

    private RecoveryPlan activeOrReplayable(RecoveryPlan plan) {
        if (plan.status() == RecoveryPlanStatus.SELECTED
                || plan.status() == RecoveryPlanStatus.EXECUTING) {
            return plan;
        }
        if (plan.status() == RecoveryPlanStatus.INVALIDATED) {
            return new RecoveryPlan(
                    plan.id(), plan.tripId(), plan.disruptionId(), plan.type(),
                    RecoveryPlanStatus.SELECTED, plan.providerId(), plan.safeStopId(),
                    List.copyOf(plan.steps()), plan.evaluation(), plan.generatedAt(), plan.selectedAt()
            );
        }
        throw new ResourceConflictException(
                "PLAN_NOT_ACTIVE", "Only selected or executing plans can be invalidated"
        );
    }

    private PlanInvalidationReason reason(String explanationCode) {
        return Arrays.stream(PlanInvalidationReason.values())
                .filter(candidate -> invalidator.explanationCode(candidate).equals(explanationCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported recovery plan invalidation explanation code"
                ));
    }
}
