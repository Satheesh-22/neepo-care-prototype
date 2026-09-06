package com.neepo.backend.service;

import com.neepo.backend.dto.BenchmarkResultRecord;
import com.neepo.backend.repository.BenchmarkResultJdbcRepository;
import com.neepo.care.benchmark.PrototypeBenchmarkReport;
import com.neepo.care.benchmark.StrategyMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BenchmarkPersistenceService {
    private static final UUID BENCHMARK_NAMESPACE = UUID.fromString(
            "e1ec9ef0-534b-5e19-91f1-4598366df12c"
    );

    private final BenchmarkResultJdbcRepository benchmarks;
    private final ObjectMapper objectMapper;

    public BenchmarkPersistenceService(
            BenchmarkResultJdbcRepository benchmarks,
            ObjectMapper objectMapper
    ) {
        this.benchmarks = Objects.requireNonNull(benchmarks, "Benchmark repository is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper is required");
    }

    @Transactional
    public int persist(UUID traceId, PrototypeBenchmarkReport report) {
        Objects.requireNonNull(traceId, "Trace id is required");
        Objects.requireNonNull(report, "Prototype benchmark report is required");
        int inserted = 0;
        for (StrategyMetrics strategy : report.strategies()) {
            BenchmarkResultRecord record = record(report, strategy);
            if (benchmarks.insertIfAbsent(record)) {
                benchmarks.insertAudit(traceId, record);
                inserted++;
            }
        }
        return inserted;
    }

    private BenchmarkResultRecord record(PrototypeBenchmarkReport report, StrategyMetrics strategy) {
        var metadata = report.metadata();
        Map<String, Object> configuration = new TreeMap<>();
        configuration.putAll(report.configuration());
        configuration.put("environment", metadata.environment());
        configuration.put("commitHash", metadata.commitHash());
        configuration.put("limitations", metadata.limitations());
        String configurationJson = json(configuration);

        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("scenarioCode", metadata.scenarioCode());
        identity.put("datasetVersion", metadata.datasetVersion());
        identity.put("measuredAt", metadata.measuredAt());
        identity.put("configuration", configuration);
        identity.put("strategy", strategy);

        return new BenchmarkResultRecord(
                deterministicId(identity),
                metadata.scenarioCode(),
                strategy.strategyCode(),
                metadata.datasetVersion(),
                strategy.successfulRecovery(),
                strategy.invalidRecommendations(),
                strategy.recoveryTimeSeconds(),
                strategy.reroutingOperations(),
                strategy.telemetryTransmissions(),
                strategy.estimatedBandwidthBytes(),
                strategy.decisionLatencyMs(),
                strategy.staleDataDecisions(),
                metadata.measuredAt(),
                configurationJson
        );
    }

    private UUID deterministicId(Object identity) {
        try {
            byte[] canonical = objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(identity)
                    .getBytes(StandardCharsets.UTF_8);
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(ByteBuffer.allocate(16)
                    .putLong(BENCHMARK_NAMESPACE.getMostSignificantBits())
                    .putLong(BENCHMARK_NAMESPACE.getLeastSignificantBits())
                    .array());
            byte[] hash = sha1.digest(canonical);
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
            ByteBuffer bytes = ByteBuffer.wrap(hash);
            return new UUID(bytes.getLong(), bytes.getLong());
        } catch (JacksonException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not derive benchmark result identity", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Benchmark configuration cannot be serialized", exception);
        }
    }
}
