# NEEPO-CARE Production Hardening Architecture & Audit

This document details the production hardening posture, security review, operational guidelines, and configuration profiles for deploying the NEEPO-CARE platform beyond local prototyping environments.

---

## 1. Secret Injection & Configuration Management

### Current Prototype State
- Development defaults are provisioned via environment variables (`NEEPO_JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, `REDIS_PASSWORD`, etc.) with fallback defaults in `application.yml` for zero-configuration local runs.
- Validation scripts accept credentials via process environment variables (e.g., `NEEPO_TEST_PASSWORD`) without hardcoding secrets in source files.

### Production Hardening Guidelines
1. **Zero Hardcoded Secrets**: In production profiles (`SPRING_PROFILES_ACTIVE=prod`), all default secrets are strictly prohibited. The application fails fast on startup if mandatory secrets (`NEEPO_JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, `NEEPO_ENCRYPTION_KEY`) match development defaults.
2. **Cloud Secret Manager / Vault Integration**:
   - In Kubernetes/Cloud environments, inject secrets dynamically via HashiCorp Vault, AWS Secrets Manager, or Google Secret Manager via CSI Secret Store drivers or runtime environment injection.
   - Secrets must never be mounted in container image layers, committed to Git repositories, or baked into Dockerfiles.
3. **Secret Rotation**:
   - JWT HMAC keys (`HS256`/`HS512`) support dual-key verification during rotation grace periods.
   - Database credentials rotate through scheduled vault lease renewals.

---

## 2. CORS (Cross-Origin Resource Sharing)

### Current Prototype State
- In local development, Nginx reverse proxy routes all requests from the same origin (`http://localhost:3000`), proxying `/api/` and `/ws` to the backend on `http://backend:8080`.
- The Spring Boot backend accepts origin requests configured via `cors.allowed-origins` (`http://localhost:3000`, `http://127.0.0.1:3000`).

### Production Hardening Guidelines
1. **Strict Whitelist**: Production deployments must restrict `cors.allowed-origins` exclusively to verified organizational hostnames (e.g., `https://care.neepo.io`, `https://driver.neepo.io`).
2. **No Wildcards**: Never allow `*` wildcard origins when `allow-credentials: true` is enabled.
3. **Allowed Headers & Methods**:
   - Limit `allowed-methods` strictly to `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
   - Limit `allowed-headers` to `Authorization, Content-Type, Accept, X-Requested-With, X-Correlation-ID`.
   - Limit `exposed-headers` to `X-Trace-Id, X-Total-Count`.

---

## 3. TLS Termination & Network Security

### Current Prototype State
- Gateway exposes HTTP port 3000 for zero-friction local developer testing and integration scripts.

### Production Hardening Guidelines
1. **Edge TLS Termination**:
   - Terminate TLS at the Ingress controller, Cloud Load Balancer (ALB / Cloudflare / GCP HTTPS LB), or edge Nginx reverse proxy using TLS 1.3 (or modern TLS 1.2 cipher suites).
   - Enforce HTTP Strict Transport Security (HSTS): `Strict-Transport-Security: max-age=63072000; includeSubDomains; preload`.
2. **Internal Service-to-Service Encryption**:
   - In zero-trust / VPC environments, enable mTLS (Mutual TLS) between reverse proxy, Spring Boot backend, PostgreSQL, and Redis.
   - Configure Spring Boot with `server.ssl.enabled=true` or rely on service mesh (e.g., Linkerd / Istio) transparent mTLS.

---

## 4. Rate Limiting & Denial-of-Service Protection

### Current Prototype State
- Local stack processes simulator bursts without request rate restrictions to facilitate rapid testing and high-frequency benchmarks.

### Production Hardening Guidelines
1. **Edge Rate Limiting (Nginx / Cloudflare / Kong)**:
   - `/api/v1/auth/login`: 5 requests per minute per IP to prevent brute-force attacks.
   - `/api/v1/events/ticket`: 30 requests per minute per user.
   - `/api/v1/telemetry`: 120 requests per minute per active vehicle token.
   - General API read endpoints: 300 requests per minute per tenant.
2. **Bucket-Based Application Throttling**:
   - Integrate Redis Token Bucket rate-limiting in Spring Cloud Gateway / Spring Filter layer for distributed multi-instance coordination.
   - Respond with HTTP 429 Too Many Requests and standard `Retry-After` headers.

---

## 5. Database Backup, Retention & Disaster Recovery

### Current Prototype State
- PostgreSQL 17 + PostGIS stores relational telemetry, trips, drivers, vehicles, cargo, and CARE decision plans in a persistent Docker volume `neepo_postgres_data`.

### Production Hardening Guidelines
1. **Automated WAL & Continuous Archiving**:
   - Enable Point-In-Time Recovery (PITR) with continuous Write-Ahead Log (WAL) archiving to encrypted object storage (e.g., S3 / GCS).
   - Take automated daily snapshot backups with a 30-day retention policy.
2. **Partitioning & Data Retention**:
   - Telemetry observations (`telemetry_observations`) must be range-partitioned by `recorded_at` (monthly partitions).
   - Archive telemetry older than 90 days to cold storage / BigQuery / data lakehouse, preserving summarized trip metrics in hot relational storage.
3. **High Availability**:
   - Deploy PostgreSQL with streaming replication (multi-AZ primary and standby replicas) and automated failover (e.g., Patroni / AWS RDS Multi-AZ / Cloud SQL HA).

---

## 6. Redis Security & Resilience

### Current Prototype State
- Redis 7 is deployed with password authentication enabled (`REDIS_PASSWORD`), isolated in the internal Docker bridge network (`neepo-network`).

### Production Hardening Guidelines
1. **Authentication & ACLs**:
   - Utilize Redis 6+ ACLs (Access Control Lists) with principle of least privilege:
     - User `neepo_backend` with permissions restricted to cache keys (`cache:*`), session keys (`session:*`), and rate-limit counters (`rl:*`).
   - Disable dangerous management commands in `redis.conf`: `FLUSHALL`, `FLUSHDB`, `CONFIG`, `DEBUG`, `SHUTDOWN`.
2. **In-Transit Encryption**:
   - Enable Redis TLS (`rediss://`) for all inter-node and client-server traffic.
3. **Eviction Policy & Memory Capping**:
   - Set `maxmemory` threshold (e.g., 80% container memory) and configure `maxmemory-policy volatile-lru` or `allkeys-lru` to prevent out-of-memory container crashes.

---

## 7. Structured Logging, Tracing & Observability

### Current Prototype State
- Spring Boot emits structured JSON/console logs.
- `TraceIdFilter` injects or propagates `X-Trace-Id` across the HTTP request lifecycle into the MDC (Mapped Diagnostic Context).
- Sensitive data (passwords, JWT secrets) are excluded from log outputs.

### Production Hardening Guidelines
1. **Distributed Tracing (OpenTelemetry & W3C TraceContext)**:
   - Propagate `traceparent` and `tracestate` across all HTTP, WebSocket, and asynchronous event boundaries.
   - Export spans and traces to OpenTelemetry Collector / Jaeger / AWS X-Ray.
2. **Log Aggregation & Masking**:
   - Format all application logs in structured JSON (e.g., Logback `LogstashEncoder`).
   - Implement log masking rules for PII (driver phone numbers, national IDs, license numbers) and credentials.
3. **Metrics & Alerting**:
   - Expose Prometheus metrics on a protected, internal-only management port (e.g., `server.port=8080`, `management.server.port=9090`).
   - Define Prometheus alert rules for:
     - High 5xx error rates (> 1% over 5m)
     - High CARE decision latency (> 250ms p95)
     - Telemetry ingestion backlog / queue lag
     - Redis memory pressure (> 85%)
     - Database connection pool exhaustion (> 90% Hikari active connections).

---

## 8. WebSocket Authentication & Lifecycle Security

### Current Prototype State
- WebSocket connections require a single-use cryptographically random ticket issued by `/api/v1/events/ticket`.
- Tickets expire after 60 seconds or immediately upon single consumption (`WebSocketTicketHandshakeInterceptor`).
- Unauthenticated or reused ticket handshake attempts are rejected with HTTP 401/403 before connection upgrade.

### Production Hardening Guidelines
1. **Connection Timeouts & Heartbeats**:
   - Configure WebSocket ping/pong keep-alive intervals (e.g., 30s) to detect dead sockets and prevent half-open TCP resource leaks.
   - Enforce idle connection maximum lifetime (e.g., 4 hours) requiring client reconnection with fresh authentication ticket.
2. **Channel Authorization**:
   - Tenant and trip isolation: verify during subscription that the authenticated user has explicit permission to monitor the requested `tripId`.
3. **Connection Concurrency Caps**:
   - Limit concurrent WebSocket connections per user (e.g., maximum 5 concurrent sockets) to prevent connection flooding attacks.

---

## 9. PWA Cache Behavior, Offline Strategy & Assets

### Current Prototype State
- Frontend built as a Progressive Web Application (PWA) with Vite, Workbox Service Worker, and IndexedDB local request queue.
- Unauthenticated landing page and core static bundles cached; API requests routed over network with offline queuing.

### Production Hardening Guidelines
1. **Cache Partitioning & Cache-Control Headers**:
   - Static assets (`/assets/*.js`, `/assets/*.css`, images): `Cache-Control: public, max-age=31536000, immutable` (fingerprinted chunks).
   - Dynamic entry point (`/index.html`): `Cache-Control: no-cache, no-store, must-revalidate`.
   - Service Worker script (`/sw.js`): `Cache-Control: no-cache, no-store, max-age=0`.
2. **IndexedDB Encryption & Eviction**:
   - Sensitive offline telemetry cached in client IndexedDB must be encrypted with device-bound encryption key or wiped upon explicit user logout.
   - Set maximum queue age (e.g., drop/reject telemetry queued offline older than 24 hours).
3. **Content Security Policy (CSP)**:
   - Enforce strict CSP headers via Nginx/Gateway:
     ```
     Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; connect-src 'self' wss: https:; img-src 'self' data:;
     ```

---

## Summary Matrix

| Domain | Prototyping Baseline | Production Target | Verification Method |
|---|---|---|---|
| **Secrets** | Env vars + dev fallbacks | Vault/K8s Secrets + no defaults | Startup validation tests |
| **CORS** | Localhost origins | Strict domain whitelist | Automated origin checks |
| **TLS** | HTTP port 3000 gateway | TLS 1.3 edge termination + mTLS | SSL Labs / OpenSSL scans |
| **Rate Limiting** | Open burst processing | Edge + Redis token bucket | Load testing / 429 validation |
| **Backups** | Docker volume persistence | WAL PITR + automated daily snapshots | Disaster recovery drills |
| **Redis** | Password on internal network | ACLs + TLS + memory LRU caps | Security scan & audit |
| **Observability** | Console MDC trace IDs | OpenTelemetry + JSON logs + Prometheus | Grafana dashboard verification |
| **WebSocket** | Single-use tickets | Tickets + channel RBAC + concurrency caps | Live penetration test suite |
| **PWA Cache** | Workbox default caching | Immutable asset hashes + strict CSP + logout purge | Browser audit & Lighthouse |
