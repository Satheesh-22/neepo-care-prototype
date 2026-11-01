# NEEPO-CARE Prototype Scope

## Purpose

The first prototype demonstrates one complete, explainable logistics disruption and recovery scenario. A simulated refrigerated truck overheats during an active trip. CARE validates the evidence, generates alternatives, rejects unsafe plans, selects a feasible plan, reacts to provider cancellation and records a deterministic re-planning decision.

## Included demonstration

1. A dispatcher creates and monitors a trip.
2. A driver starts the assigned trip.
3. The simulator sends GPS and engine-temperature observations.
4. Engine temperature exceeds the configured safe limit.
5. CARE detects `VEHICLE_OVERHEATING`.
6. CARE generates at least three recovery plans.
7. Hard constraints reject incompatible or unsafe plans.
8. Feasible plans are scored for benefit, risk and uncertainty.
9. CARE selects and explains the highest-utility feasible plan.
10. The selected provider cancels.
11. CARE invalidates the selected plan.
12. CARE automatically generates and selects a replacement.
13. Telemetry frequency changes according to the operational state.
14. Metrics compare CARE against nearest-provider selection.

## Roles

- Dispatcher: creates trips, monitors telemetry and disruptions, compares plans and follows recovery.
- Driver: starts a trip, reports conditions, follows recovery instructions and works through temporary network loss.
- Administrator: manages platform users and configuration.
- CARE engine: validates context, detects disruptions, evaluates plans, explains decisions and re-plans.

## Excluded from the prototype

- Machine learning
- Payment processing
- Real truck or IoT hardware
- Kafka and Kubernetes
- Native mobile application
- Paid map services
- Multiple independently deployed microservices

## Completion criteria

The prototype is complete only when the end-to-end scenario runs from repeatable seed data and demonstrates:

- Continuous telemetry and idempotent event processing
- Automatic overheating detection
- At least three ordered recovery plans
- Hard-constraint rejection
- Freshness and confidence evaluation
- Deterministic plan ranking and stable tie-breaking
- Human-readable selection and rejection explanations
- Provider cancellation, invalidation and automatic re-planning
- Adaptive telemetry intervals
- Passing backend, frontend and integration tests
- Recorded baseline metrics and dated invention evidence

## v1.1 Extended Production Scope & Enterprise Capabilities

All eight v1.1 roadmap milestones have been implemented, integrated, and verified on `feature/full-stack-integration`:

1. **High-Resolution Telemetry History & Route Replay (Milestone 1)**: ISO-8601 bounded time-series scrubber, Leaflet breadcrumb trail, dynamic vehicle tracking, and $1\times, 2\times, 5\times$ speed multipliers.
2. **Multi-Channel Real-Time Alerting & Webhooks Engine (Milestone 2 - Flyway V8)**: HMAC-SHA256 signature verification (`X-Neepo-Signature`), severity threshold suppression, secret masking, and delivery logs.
3. **Cryptographic Audit & Compliance Evidence Exports (Milestone 3 - Flyway V9)**: Multi-format packaging (`JSON_LD`, `CSV`, `PRINTABLE_HTML`), SHA-256 tamper-evident digest verification, and FDA 21 CFR Part 11 / EU GDP guidelines alignment.
4. **Fleet Reliability & Cost-Avoidance Analytics (Milestone 4 - Flyway V10)**: Dynamic timeframe aggregation (7D, 30D, 90D), MTTR, driver compliance, cargo cost avoidance, telematics root causes, and CARE strategy efficiency.
5. **Hardware Gateway Fleet Pilot & Offline Resilience (Milestone 5 - Flyway V11)**: ESP32-S3 edge gateway provisioning, ATECC608A cryptographic signature verification, Winbond 32MB SPI flash circular queue offline buffering, idempotent deduplication, and dynamic safety/cold-chain envelope resolution.
6. **Multi-Tenant Enterprise Organization & SSO Integration (Milestone 6 - Flyway V12)**: OIDC & SAML 2.0 federation, domain discovery, JIT user provisioning, password bypass blocking, live IdP latency diagnostics, and tamper-evident SSO audit ledger.
7. **Hierarchical Multi-Org Fleet Partitioning & Automated SCIM 2.0 Provisioning (Milestone 7 - Flyway V13)**: Cold-chain depot boundary partitioning, facility vehicle/driver stationing, capacity utilization tracking, RFC 7643 / RFC 7644 user lifecycle synchronization, ServiceProviderConfig & Schemas, Bearer token management, and interactive `DepotsPage.tsx` console.
8. **Accessibility (WCAG 2.1 AA) Compliance & Multi-Language i18n Localization (Milestone 8)**: Skip-to-content keyboard bypass, ARIA landmarks, polite & assertive `LiveAnnouncer` live regions for telematics alerts, high-contrast 7:1+ theme mode, large text scaling, 100% parity English and Spanish translation bundles, parameter interpolation, language toggle, and accessibility preferences menu.

## Scope control

Shared contracts and this scope require team review before change. A milestone must not claim a planned endpoint or mock workflow is implemented.
