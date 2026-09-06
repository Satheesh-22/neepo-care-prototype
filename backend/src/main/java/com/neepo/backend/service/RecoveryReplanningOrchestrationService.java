package com.neepo.backend.service;

import com.neepo.backend.dto.ReplanningCycleRecord;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.invalidation.RecoveryPlanInvalidationResult;
import com.neepo.care.orchestration.RecoveryReplanner;
import com.neepo.contracts.TripContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class RecoveryReplanningOrchestrationService {
    private final DisruptionJdbcRepository disruptions;
    private final TripContextSnapshotService contexts;
    private final RecoveryReplanner replanner;
    private final ReplanningLifecycleService lifecycles;

    public RecoveryReplanningOrchestrationService(
            DisruptionJdbcRepository disruptions,
            TripContextSnapshotService contexts,
            RecoveryReplanner replanner,
            ReplanningLifecycleService lifecycles
    ) {
        this.disruptions = Objects.requireNonNull(disruptions, "Disruption repository is required");
        this.contexts = Objects.requireNonNull(contexts, "Trip context service is required");
        this.replanner = Objects.requireNonNull(replanner, "Recovery replanner is required");
        this.lifecycles = Objects.requireNonNull(lifecycles, "Replanning lifecycle service is required");
    }

    @Transactional
    public ReplanningCycleRecord complete(
            UUID companyId,
            UUID traceId,
            RecoveryPlanInvalidationResult invalidation,
            ReplanningCycleRecord cycle
    ) {
        var trigger = invalidation.replanningTrigger();
        var disruption = disruptions.findActiveByTripId(companyId, trigger.tripId())
                .orElseThrow(() -> new ResourceConflictException(
                        "ACTIVE_DISRUPTION_NOT_FOUND",
                        "An active disruption is required for recovery replanning"
                ));
        if (!disruption.id().equals(trigger.disruptionId())) {
            throw new IllegalArgumentException("Replanning trigger does not match the active disruption");
        }

        TripContext previous = contexts.load(companyId, trigger.tripId(), disruption.contextSnapshotId());
        TripContext replanningContext = new TripContext(
                previous.trip(),
                previous.vehicle(),
                previous.driver(),
                previous.cargo(),
                previous.route(),
                previous.providers(),
                previous.safeStops(),
                previous.telemetryObservations(),
                previous.networkState(),
                previous.activeDisruptions(),
                invalidation.invalidatedPlan(),
                trigger.requestedAt(),
                replanningHash(previous.contextHash(), invalidation.invalidation().id())
        );
        var snapshot = contexts.persist(companyId, trigger.tripId(), replanningContext);
        var result = replanner.replan(replanningContext, trigger, new DetectedDisruption(
                disruption.type(),
                disruption.severity(),
                disruption.detectedAt(),
                disruption.explanationCodes(),
                disruption.evidence()
        ));
        return lifecycles.complete(companyId, snapshot.id(), traceId, cycle.id(), result);
    }

    private static String replanningHash(String previousHash, UUID invalidationId) {
        try {
            byte[] value = (previousHash + ":" + invalidationId).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for replanning context hashes", exception);
        }
    }
}
