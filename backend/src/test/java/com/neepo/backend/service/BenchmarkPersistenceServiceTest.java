package com.neepo.backend.service;

import com.neepo.backend.dto.BenchmarkResultRecord;
import com.neepo.backend.repository.BenchmarkResultJdbcRepository;
import com.neepo.care.benchmark.BenchmarkMetadata;
import com.neepo.care.benchmark.PrototypeBenchmarkReport;
import com.neepo.care.benchmark.StrategyMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BenchmarkPersistenceServiceTest {
    private static final UUID TRACE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-24T12:30:00Z");

    private BenchmarkResultJdbcRepository repository;
    private BenchmarkPersistenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(BenchmarkResultJdbcRepository.class);
        service = new BenchmarkPersistenceService(repository, JsonMapper.builder().build());
    }

    @Test
    void persistsAndAuditsEveryNewStrategyWithProvenance() {
        when(repository.insertIfAbsent(any())).thenReturn(true);

        assertEquals(2, service.persist(TRACE_ID, report(configuration())));

        ArgumentCaptor<BenchmarkResultRecord> records = ArgumentCaptor.forClass(BenchmarkResultRecord.class);
        verify(repository, org.mockito.Mockito.times(2)).insertIfAbsent(records.capture());
        assertNotEquals(records.getAllValues().get(0).id(), records.getAllValues().get(1).id());
        assertTrue(records.getAllValues().get(0).configurationJson().contains("commitHash"));
        assertTrue(records.getAllValues().get(0).configurationJson().contains("limitations"));
        verify(repository, org.mockito.Mockito.times(2)).insertAudit(
                org.mockito.ArgumentMatchers.eq(TRACE_ID), any(BenchmarkResultRecord.class)
        );
    }

    @Test
    void exactReportReplayUsesStableIdsAndDoesNotDuplicateAudits() {
        PrototypeBenchmarkReport report = report(configuration());
        when(repository.insertIfAbsent(any())).thenReturn(true);
        service.persist(TRACE_ID, report);
        ArgumentCaptor<BenchmarkResultRecord> first = ArgumentCaptor.forClass(BenchmarkResultRecord.class);
        verify(repository, org.mockito.Mockito.times(2)).insertIfAbsent(first.capture());

        reset(repository);
        when(repository.insertIfAbsent(any())).thenReturn(false);
        assertEquals(0, service.persist(TRACE_ID, report));
        ArgumentCaptor<BenchmarkResultRecord> replay = ArgumentCaptor.forClass(BenchmarkResultRecord.class);
        verify(repository, org.mockito.Mockito.times(2)).insertIfAbsent(replay.capture());

        assertEquals(
                first.getAllValues().stream().map(BenchmarkResultRecord::id).toList(),
                replay.getAllValues().stream().map(BenchmarkResultRecord::id).toList()
        );
        verify(repository, never()).insertAudit(any(), any());
    }

    @Test
    void configurationMapOrderCannotChangeDeterministicIdentity() {
        when(repository.insertIfAbsent(any())).thenReturn(true);
        service.persist(TRACE_ID, report(configuration()));
        ArgumentCaptor<BenchmarkResultRecord> forward = ArgumentCaptor.forClass(BenchmarkResultRecord.class);
        verify(repository, org.mockito.Mockito.times(2)).insertIfAbsent(forward.capture());

        reset(repository);
        LinkedHashMap<String, String> reversed = new LinkedHashMap<>();
        reversed.put("payloadBytes", "512");
        reversed.put("fixedInterval", "60");
        when(repository.insertIfAbsent(any())).thenReturn(true);
        service.persist(TRACE_ID, report(reversed));
        ArgumentCaptor<BenchmarkResultRecord> backward = ArgumentCaptor.forClass(BenchmarkResultRecord.class);
        verify(repository, org.mockito.Mockito.times(2)).insertIfAbsent(backward.capture());

        assertEquals(
                forward.getAllValues().stream().map(BenchmarkResultRecord::id).toList(),
                backward.getAllValues().stream().map(BenchmarkResultRecord::id).toList()
        );
    }

    private PrototypeBenchmarkReport report(Map<String, String> configuration) {
        BenchmarkMetadata metadata = new BenchmarkMetadata(
                "OVERHEAT-001", "demo-v1", "Java 21 test", "52598a9", NOW,
                List.of("Deterministic fixture")
        );
        return new PrototypeBenchmarkReport(metadata, configuration, List.of(
                new StrategyMetrics("CARE_ADAPTIVE", true, 0, 900, 1, 13, 6656, 12, 0),
                new StrategyMetrics("FIXED_BASELINE", false, 1, 600, 1, 5, 2560, 2, 1)
        ));
    }

    private Map<String, String> configuration() {
        LinkedHashMap<String, String> configuration = new LinkedHashMap<>();
        configuration.put("fixedInterval", "60");
        configuration.put("payloadBytes", "512");
        return configuration;
    }
}
