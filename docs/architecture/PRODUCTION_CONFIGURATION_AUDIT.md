# NEEPO-CARE Production Configuration Audit

This document provides a comprehensive audit of the production configuration, security boundaries, credential lifecycles, and environment isolation for the NEEPO-CARE platform.

---

## 1. Environment-Based Secret Injection

### Baseline Architecture
All operational secrets are decoupled from application source code and Docker container images. Values are provided strictly at runtime via environment variables or cloud secret injectors.

| Secret Key | Target Service | Default (Local / Dev) | Production Strategy |
|---|---|---|---|
| `JWT_SECRET` | Backend (`neepo.security.jwt-secret`) | Provided via `.env` (min 32 bytes) | Injected via Secret Manager / Vault (256-bit+ entropy) |
| `DATABASE_PASSWORD` | Backend (`spring.datasource.password`) | Provided via `.env` | Secret Manager / IAM database token |
| `POSTGRES_PASSWORD` | PostgreSQL Container | Provided via `.env` | Vault provisioned / RDS master secret |
| `REDIS_PASSWORD` | Redis / Backend | Provided via `.env` | Vault dynamic credential / AWS ElastiCache Auth |
| `NEEPO_TEST_PASSWORD` | Validation Scripts | None (set per test run) | Ephemeral test runner environment secret |

### Rules & Validation
- Secrets are **never** committed to version control, Dockerfiles, or embedded in client JS bundles.
- Startup fails fast (`IllegalStateException`) if `JWT_SECRET` is less than 32 UTF-8 bytes.
- In production profile (`SPRING_PROFILES_ACTIVE=prod`), fallback defaults are disabled.

---

## 2. JWT Key Management & Lifecycle

### Cryptographic Standard
- **Algorithm**: HMAC-SHA256 (`HmacSHA256`) via Nimbus JOSE + Spring Security OAuth2 Resource Server.
- **Key Length**: Minimum 256 bits (32 bytes); recommended 512 bits for production.
- **Claims Issued**:
  - `sub`: User ID (UUID)
  - `tenantId`: Organization tenant identifier (UUID)
  - `role`: User role (`ADMIN`, `DISPATCHER`, `DRIVER`)
  - `iss`: `neepo-care`
  - `iat`: Issue timestamp (UTC)
  - `exp`: Expiration timestamp (configurable, default 60 minutes)

### Key Rotation Strategy
1. **Primary & Secondary Keys**: Production supports primary signing key and secondary verification key for seamless key rollover without disconnecting active sessions.
2. **Short-Lived Access Tokens**: 15–60 minute access token validity limits exposure if a token is intercepted.

---

## 3. Database Credentials & Connection Security

### PostgreSQL / PostGIS Hardening
- **Application User**: Dedicated `neepo_app` user with `SELECT`, `INSERT`, `UPDATE`, `DELETE` grants only on application tables. DDL operations reserved for Flyway migration runner.
- **Connection URL**: Enforce TLS encryption in transit:
  ```
  jdbc:postgresql://postgres.production.internal:5432/neepo_care?sslmode=verify-full&sslrootcert=/etc/ssl/certs/pg-ca.pem
  ```
- **Connection Pool (HikariCP)**:
  - `maximum-pool-size`: 20 connections per backend replica.
  - `minimum-idle`: 5 connections.
  - `idle-timeout`: 300,000 ms (5 minutes).
  - `connection-timeout`: 20,000 ms (20 seconds).
  - `leak-detection-threshold`: 15,000 ms.

---

## 4. Redis Credentials & In-Memory Isolation

### Access Control & Commands
- **Authentication**: `requirepass` enabled in `redis.conf` with strong alphanumeric passphrase.
- **Network Isolation**: Redis bound strictly to internal private network (`127.0.0.1` or VPC private subnet); port 6379 never exposed to public ingress.
- **Restricted Commands**: Hazardous administrative commands renamed/disabled:
  ```text
  rename-command FLUSHALL ""
  rename-command FLUSHDB ""
  rename-command CONFIG ""
  rename-command SHUTDOWN ""
  ```
- **TLS**: Production cluster configured with `rediss://` encrypted connections.

---

## 5. CORS Policy & Allowed Frontend Origins

### Same-Origin Architecture
- In standard deployment, Nginx edge proxy serves static PWA assets on port 443/3000 and reverse-proxies `/api/` and `/ws` to the backend on port 8080, establishing a strict **same-origin** security context.

### Cross-Origin Rules (When Multi-Domain)
When backend APIs are hosted on dedicated API domains (e.g., `api.neepo.io` vs `app.neepo.io`):
- **Allowed Origins**: Strictly whitelisted domain names (e.g. `https://app.neepo.io`, `https://driver.neepo.io`).
- **Wildcard Prohibition**: `Access-Control-Allow-Origin: *` is strictly forbidden when credentials/cookies/tokens are exchanged.
- **Allowed Methods**: `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- **Allowed Headers**: `Authorization, Content-Type, Accept, X-Requested-With, X-Correlation-ID`.

---

## 6. Standardized Error Responses & Information Hiding

### Error Contract
All failure responses conform to the uniform `ApiErrorResponse` envelope:
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Trip not found: 00000000-0000-4000-8600-000000000001",
    "fieldErrors": []
  },
  "timestamp": "2026-09-06T14:15:00.123Z",
  "traceId": "c4b8b54e-37f2-494b-bf50-e144498877bc"
}
```

### Information Leakage Prevention
- **No Stack Traces**: JVM stack traces, internal SQL exceptions, and database class names are intercepted by `GlobalExceptionHandler` and hidden from HTTP responses.
- **Opaque Authentication Errors**: Failed logins return generic `INVALID_CREDENTIALS` (HTTP 401) without revealing whether the email or password was incorrect.

---

## 7. Distributed Tracing & MDC Correlation

### Request Trace ID Lifecycle
1. `TraceIdFilter` checks incoming request for `X-Trace-Id` or `X-Correlation-Id`. If absent, generates a cryptographically secure UUID v4.
2. Injects `traceId` into SLF4J / Logback MDC (Mapped Diagnostic Context).
3. Adds `X-Trace-Id` header to HTTP response.
4. Includes `traceId` in all structured error payloads for immediate log correlation in Sentry/Datadog/Elasticsearch.

---

## 8. Logging Redaction & Privacy

### Sanitization Rules
- **Passwords**: Excluded from logs and serialized JSON outputs (`@JsonProperty(access = Access.WRITE_ONLY)`).
- **JWT Secrets & Tokens**: Never logged; authorization headers stripped before request logging.
- **PII Protection**: Driver telephone numbers and national license numbers are masked or pseudonymized in audit logs.
- **Nginx Redaction**: Nginx access logs explicitly disable logging query parameters on `/ws` to prevent recording WebSocket tickets on disk.

---

## 9. WebSocket Authentication & Lifecycle Security

### Single-Use Ticket Protocol
1. Client requests ticket via authenticated POST `/api/v1/events/ticket` with Bearer JWT.
2. Server issues cryptographically random token stored with 60-second TTL.
3. Client opens WebSocket connection: `GET /ws?tripId={id}&ticket={token}`.
4. Handshake interceptor validates ticket, immediately consumes and deletes it (preventing replay attacks), and associates connection with authorized tenant and trip ID.
5. Unauthenticated or expired handshakes receive HTTP 401/403 before protocol upgrade.

---

## 10. Profile Matrix: Development vs Production

| Configuration Parameter | `local` / `dev` | `staging` | `prod` |
|---|---|---|---|
| **Database DDL** | Hibernate `validate` + Flyway | Hibernate `validate` + Flyway | Hibernate `validate` + Flyway |
| **Log Level** | `DEBUG` / `INFO` | `INFO` | `INFO` / `WARN` (JSON) |
| **TLS Enforcement** | Disabled (plain HTTP) | Enabled (Edge SSL) | Enabled (Edge TLS 1.3 + mTLS) |
| **Actuator Endpoints** | `health,info` (unauthenticated) | `health,info,prometheus` | Protected internal port only |
| **JWT Expiration** | 60 minutes | 30 minutes | 15 minutes + refresh |
| **CORS Origins** | `localhost:3000`, `127.0.0.1:3000` | Staging domains | Whitelisted production FQDNs |
| **Cache Headers** | Dev caching | Immutable with hash | Immutable asset caching + strict CSP |

---

## 11. Production Verification Checklist

- [x] All application credentials injected via environment variables with zero checked-in secrets.
- [x] JWT signing key requires $\ge 256$ bits of entropy.
- [x] PostgreSQL connection enforces SSL in transit and least-privilege database user.
- [x] Redis connection isolated to internal private network.
- [x] WebSocket handshake authenticated via single-use, 60-second expiring tickets.
- [x] Trace ID propagated across all HTTP, error, and log contexts.
- [x] Public error envelopes strip internal system traces and SQL details.
