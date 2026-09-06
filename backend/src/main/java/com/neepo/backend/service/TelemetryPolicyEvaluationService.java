package com.neepo.backend.service;

import com.neepo.backend.dto.TelemetryPolicyRequest;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.backend.repository.TripContextSnapshotJdbcRepository;
import com.neepo.care.telemetry.AdaptiveTelemetryPolicyEvaluator;
import com.neepo.care.telemetry.TelemetryPolicyDecision;
import com.neepo.care.telemetry.TelemetryPolicySignal;
import com.neepo.contracts.ActiveDisruption;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.TripContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TelemetryPolicyEvaluationService {
    private final TripContextSnapshotJdbcRepository snapshotRepository;
    private final TripContextSnapshotService snapshots;
    private final DisruptionJdbcRepository disruptions;
    private final RecoveryQueryJdbcRepository recovery;
    private final AdaptiveTelemetryPolicyEvaluator evaluator;
    private final TelemetryPolicyPersistenceService persistence;

    public TelemetryPolicyEvaluationService(
            TripContextSnapshotJdbcRepository snapshotRepository,
            TripContextSnapshotService snapshots,
            DisruptionJdbcRepository disruptions,
            RecoveryQueryJdbcRepository recovery,
            AdaptiveTelemetryPolicyEvaluator evaluator,
            TelemetryPolicyPersistenceService persistence
    ) {
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "Snapshot repository is required");
        this.snapshots = Objects.requireNonNull(snapshots, "Snapshot service is required");
        this.disruptions = Objects.requireNonNull(disruptions, "Disruption repository is required");
        this.recovery = Objects.requireNonNull(recovery, "Recovery repository is required");
        this.evaluator = Objects.requireNonNull(evaluator, "Telemetry policy evaluator is required");
        this.persistence = Objects.requireNonNull(persistence, "Telemetry policy persistence is required");
    }

    @Transactional
    public TelemetryPolicyDecision evaluate(
            UUID companyId,
            UUID tripId,
            UUID traceId,
            TelemetryPolicyRequest request
    ) {
        var latest = snapshotRepository.findLatest(companyId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CONTEXT_SNAPSHOT_NOT_FOUND",
                        "A telemetry context snapshot is required before policy evaluation"
                ));
        TripContext previous = snapshots.load(companyId, tripId, latest.id());
        var disruption = disruptions.findActiveByTripId(companyId, tripId);
        List<ActiveDisruption> activeDisruptions = disruption
                .map(value -> List.of(new ActiveDisruption(
                        value.id(), value.eventId(), value.type(), value.severity(), value.detectedAt(),
                        value.explanationCodes(), value.evidence()
                )))
                .orElseGet(List::of);
        var currentPlan = recovery.findPlans(companyId, tripId).stream()
                .filter(plan -> plan.status() == RecoveryPlanStatus.SELECTED
                        || plan.status() == RecoveryPlanStatus.EXECUTING)
                .findFirst()
                .orElse(null);
        Instant evaluationTime = previous.evaluationTime().isAfter(request.observedAt())
                ? previous.evaluationTime()
                : request.observedAt();
        TripContext context = new TripContext(
                previous.trip(), previous.vehicle(), previous.driver(), previous.cargo(), previous.route(),
                previous.providers(), previous.safeStops(), previous.telemetryObservations(), previous.networkState(),
                activeDisruptions, currentPlan, evaluationTime,
                policyContextHash(previous.contextHash(), request.eventId())
        );
        var snapshot = snapshots.persist(companyId, tripId, context);
        TelemetryPolicyDecision decision = evaluator.evaluate(context, new TelemetryPolicySignal(
                request.eventId(), request.observedAt(), request.nearDecisionPoint(), request.safelyParked()
        ));
        persistence.persist(companyId, traceId, decision);
        if (!snapshot.tripId().equals(decision.tripId())) {
            throw new IllegalStateException("Telemetry policy snapshot does not match its decision");
        }
        return decision;
    }

    private static String policyContextHash(String previousHash, UUID eventId) {
        try {
            byte[] input = (previousHash + ":telemetry-policy:" + eventId).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for telemetry policy context hashes", exception);
        }
    }
}
