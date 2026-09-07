# NEEPO-CARE Observability & Operations Architecture

This document outlines the production observability stack, metric instrumentation, alert thresholds, structured logging standards, and incident runbooks for the NEEPO-CARE logistics disruption recovery platform.

---

## 1. Observability Pillars

```
+-----------------------------------------------------------------------------+
|                               NEEPO-CARE STACK                              |
|   [ Frontend (Nginx/React) ]   [ Backend (Spring Boot) ]   [ Postgres/Redis]|
+-----------------------------------------------------------------------------+
          |                                  |                        |
          v                                  v                        v
+-----------------------+          +--------------------+   +-----------------+
|   OpenTelemetry /     |          | Prometheus Metrics |   | Structured JSON |
|   Distributed Traces  |          | /actuator/prom     |   | Logs (MDC/Logback)
+-----------------------+          +--------------------+   +-----------------+
          |                                  |                        |
          v                                  v                        v
+-----------------------+          +--------------------+   +-----------------+
| Jaeger / Cloud Trace  |          | Grafana Dashboards |   | Loki / Cloud    |
| (W3C TraceContext)    |          | & Alertmanager     |   | Logging (ELK)   |
+-----------------------+          +--------------------+   +-----------------+
```

---

## 2. Metric Specifications

All backend services expose standardized Prometheus metrics via `/actuator/prometheus`.

| Metric Name | Type | Unit | Description | Target SLA |
|---|---|---|---|---|
| `http_server_requests_seconds` | Histogram | seconds | Request latency & HTTP status breakdown | p95 < 200ms |
| `care_decision_duration_seconds` | Histogram | seconds | CARE disruption solver computation time | p95 < 250ms |
| `neepo_telemetry_events_ingested_total` | Counter | count | Total telemetry batches ingested | > 0 on active trips |
| `neepo_telemetry_last_ingest_timestamp_seconds` | Gauge | unix epoch | Timestamp of most recent telemetry event | Lag < 60s |
| `neepo_websocket_active_sessions` | Gauge | count | Active STOMP/WebSocket connected clients | Dynamic |
| `neepo_disruption_events_broadcast_total` | Counter | count | Real-time disruption alerts published | Real-time |
| `hikaricp_connections_active` | Gauge | count | Active PostgreSQL connections | < 85% of max |
| `redis_memory_used_bytes` | Gauge | bytes | Redis memory footprint | < 80% of max |
| `neepo_offline_replay_errors_total` | Counter | count | Offline batch replay sync errors | 0 |

---

## 3. Production Alerting Thresholds

Alert rules are defined declaratively in `deploy/observability/prometheus-rules.yaml`.

| Alert Name | Severity | Condition | Evaluation Window | Action Required |
|---|---|---|---|---|
| `NeepoCareHighErrorRate` | **Critical (P1)** | HTTP 5xx rate > 1% | 3 minutes | Page On-Call, inspect backend container logs |
| `NeepoCareSolverLatencySpike` | **Warning (P2)** | Solver p95 > 250ms | 3 minutes | Check candidate solver pool, DB lock contention |
| `NeepoCareTelemetryLagDetected` | **Warning (P2)** | Telemetry lag > 60s | 2 minutes | Inspect sensor gateway health and broker queue |
| `NeepoCareDbConnectionPoolNearCapacity` | **Warning (P2)** | Active pool > 85% | 2 minutes | Check slow queries, connection leaks |
| `NeepoCareRedisMemoryHigh` | **Warning (P2)** | Used memory > 80% | 5 minutes | Review cache TTL policies, trigger memory evictions |
| `NeepoCareOfflineReplayFailures` | **Critical (P1)** | Replay failures > 5 | 1 minute | Inspect malformed telemetry payloads in DLQ |
| `NeepoCareAuthFailureBurst` | **Warning (P2)** | Auth failures > 20 | 2 minutes | Check for brute force attacks or expired secret rotation |

---

## 4. Structured Logging & Trace Correlation

All logs emitted by backend services are formatted in structured JSON or pattern-enriched logs with MDC (Mapped Diagnostic Context) attributes:

```json
{
  "timestamp": "2026-09-06T14:30:00.123Z",
  "level": "INFO",
  "logger": "com.neepo.care.service.CareRecoveryPlanner",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "tenantId": "org-fleet-alpha",
  "tripId": "trip-882194",
  "userId": "dispatcher-441",
  "message": "Generated 3 recovery candidate options; selected option A (score 94.2)"
}
```

### Secret Sanitization Guarantee:
- Passwords, JWT secrets, authorization tokens, and private keys are never logged.
- Query parameters and request bodies containing sensitive credentials are sanitized at the servlet filter layer.

---

## 5. SRE Incident Runbooks

### 5.1 CARE Solver Latency Spike (Runbook-01)
1. Check `/actuator/metrics/care_decision_duration_seconds`.
2. Inspect whether the vehicle is in a dense cluster with $>50$ candidate facilities.
3. Check PostgreSQL slow query logs for `ST_DWithin` spatial query performance on `recovery_options` or `safe_haven_locations`.
4. If necessary, tune `neepo.care.solver.candidate-limit` via dynamic config.

### 5.2 Telemetry Ingestion Stall (Runbook-02)
1. Verify hardware sensor gateway connectivity and ingress network policies.
2. Check `neepo-care-prototype-postgres-1` lock stats.
3. Query `SELECT max(recorded_at) FROM trip_telemetry;` to confirm database commit state.
4. If sensor gateway is offline, check device flash buffer status.
