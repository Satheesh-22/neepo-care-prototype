# NEEPO-CARE Security Hardening Guide

This document outlines the security controls, policies, configurations, and implementation guidelines for deploying NEEPO-CARE in security-critical and production environments.

---

## 1. TLS Termination & Transport Security

### Edge TLS Configuration
- **Protocol**: Enforce TLS 1.3 as primary; TLS 1.2 permitted with strong forward-secrecy cipher suites:
  - `ECDHE-ECDSA-AES256-GCM-SHA384`
  - `ECDHE-RSA-AES256-GCM-SHA384`
  - `ECDHE-ECDSA-CHACHA20-POLY1305`
  - `ECDHE-RSA-CHACHA20-POLY1305`
- **HTTP Strict Transport Security (HSTS)**:
  ```nginx
  add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
  ```
- **Automated Certificate Renewal**: Utilize Let's Encrypt / Certbot or AWS Certificate Manager (ACM) with automated DNS-01 or HTTP-01 renewal.

---

## 2. Secure HTTP Headers & Gateway Controls

The Nginx edge reverse proxy injects standard hardening headers on all HTTP responses:

```nginx
client_max_body_size 10M;

add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(self), microphone=(), camera=()" always;
```

### Content Security Policy (CSP)
For production PWA delivery:
```text
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; connect-src 'self' wss: https:; img-src 'self' data:; frame-ancestors 'none';
```

---

## 3. Rate Limiting for Authentication & Telemetry

### Edge Rate Limiting (Nginx Ingress / API Gateway)
Define rate limiting zones in the edge proxy configuration:

```nginx
# Rate limiting zones
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=5r/m;
limit_req_zone $http_authorization zone=telemetry_limit:10m rate=60r/m;
limit_req_zone $binary_remote_addr zone=api_general:10m rate=300r/m;

server {
    # Apply strict rate limits to login
    location = /api/v1/auth/login {
        limit_req zone=auth_limit burst=5 nodelay;
        limit_req_status 429;
        proxy_pass http://backend:8080;
    }

    # Telemetry ingestion bursts allowed with buffering
    location = /api/v1/telemetry {
        limit_req zone=telemetry_limit burst=20 nodelay;
        limit_req_status 429;
        proxy_pass http://backend:8080;
    }

    # General APIs
    location /api/ {
        limit_req zone=api_general burst=50 nodelay;
        limit_req_status 429;
        proxy_pass http://backend:8080;
    }
}
```

---

## 4. Strict CORS & Request Boundary Enforcement

- When deploying backend API and frontend on separate hostnames:
  - Configure `cors.allowed-origins` with explicit domain list (e.g., `https://care.neepo.io`, `https://driver.neepo.io`).
  - Disallow wildcard `*` origins whenever `allow-credentials: true` is configured.
  - Reject unexpected HTTP verbs (`TRACE`, `TRACK`, `CONNECT`).
- Request body sizes are capped at **10MB** both at Nginx (`client_max_body_size 10M;`) and Spring Boot (`spring.servlet.multipart.max-request-size: 10MB`).

---

## 5. WebSocket Origin & Ticket Validation

### Handshake Protection Sequence
1. WebSocket connection requests to `/ws` require a query parameter `ticket`.
2. Handshake interceptor (`WebSocketTicketHandshakeInterceptor`):
   - Validates that the ticket exists in the active cache.
   - Immediately invalidates and removes the ticket upon receipt (single-use semantics).
   - Validates the `Origin` header against permitted frontend domains.
   - Associates the WebSocket session with the authenticated tenant ID and trip ID.
3. If the ticket is absent, expired, already consumed, or origin invalid, the connection is rejected with HTTP 401/403 before upgrade.

---

## 6. Token Expiration, Storage & Session Security

- **Access Token TTL**: Configurable via `JWT_EXPIRATION_MINUTES` (production recommendation: 15–30 minutes).
- **Client Token Storage**:
  - Web applications store tokens in memory with refresh mechanisms or in secure HttpOnly SameSite=Strict cookies.
  - Mobile/PWA offline tokens stored in encrypted IndexedDB / secure platform keystore.
- **Session Termination**:
  - Logout purges local credentials, clears cached offline telemetry, and disconnects all active WebSocket subscriptions.

---

## 7. Audit Logging for Privileged Operations

### Tracked Security Events
The platform logs structured audit events with user ID, tenant ID, client IP, and trace ID for:
1. **Authentication**: Successful logins, failed login attempts (with reason code), token refresh.
2. **Trip Lifecycle**: Trip creation, vehicle/driver assignment, manual trip start/completion.
3. **CARE Decisions**: Recovery plan generation, deterministic plan selection, manual dispatcher plan override/invalidation.
4. **Provider Operations**: Provider availability status updates, emergency provider dispatch.
5. **Security Management**: User provisioning, role modifications, tenant configuration changes.

All audit logs are tagged with `audit=true` and correlated with the global `traceId`.
