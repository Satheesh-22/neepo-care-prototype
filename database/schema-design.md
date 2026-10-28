# NEEPO-CARE Database Design

## Decisions

- PostgreSQL 17 with PostGIS 3.5
- UUID primary keys
- UTC `TIMESTAMPTZ`
- Flyway-only schema changes
- Optimistic locking through `version BIGINT`
- `geography(Point,4326)` and `geography(LineString,4326)` for spatial data
- JSONB only for snapshots, flexible evidence and detailed result maps
- Core statuses, scores, timestamps and relationships remain typed relational columns

The executable physical schema is in:

1. `V1__create_identity_foundation.sql`
2. `V2__create_operational_domain.sql`
3. `V3__create_context_and_decision_domain.sql`
4. `V4__seed_repeatable_demo_data.sql`

## Table catalogue

| Table | Primary purpose | Critical keys/constraints |
|---|---|---|
| `companies` | Tenant/company boundary | Unique company code |
| `users` | Platform identities and roles | Case-insensitive unique email; checked role |
| `drivers` | Driver status and legal time | Unique user/license; remaining minutes bounded |
| `vehicles` | Dimensions, capability and health | Unique registration; health 0–100 |
| `vehicle_constraints` | Vehicle hard/soft requirements | Unique vehicle + code |
| `cargo` | Cargo type, weight and temperature range | Unique reference; valid min/max |
| `cargo_constraints` | Cargo-specific hard/soft requirements | Unique cargo + code |
| `routes` | Origin, destination and planned path | Spatial fields and positive plan values |
| `route_restrictions` | Height/width/weight/blockage evidence | Observation/expiry/confidence retained |
| `trips` | Vehicle-driver-cargo-route assignment | Checked status, deadlines and wait limits |
| `trip_assignments` | Assignment history | Valid assignment/release period |
| `service_providers` | Workshop/mechanic/tow/substitute resources | Checked provider type and score |
| `provider_capabilities` | Confirmed provider capabilities | Unique provider + capability |
| `provider_availability` | Time-bounded provider state | Status, observed/expiry, ETA, confidence |
| `safe_stops` | Verified stopping locations | Dimensions, security, freshness/confidence |
| `telemetry_observations` | Immutable sensor/GPS events | Unique event ID; recorded/received/expiry |
| `trip_context_snapshots` | Reproducible engine input | Trip sequence, input hash, context JSONB |
| `disruption_events` | Deterministic detections | Unique event ID; context evidence |
| `recovery_plans` | Candidate/selected plan header | Stable supplied UUID; one active selection |
| `recovery_plan_steps` | Ordered execution instructions | Unique plan + sequence |
| `plan_evaluations` | Transparent constraint and utility result | One evaluation per plan; all score terms stored |
| `plan_invalidations` | Why selected plan became invalid | Unique triggering event ID |
| `replanning_cycles` | Automatic re-planning history | Unique disruption + cycle |
| `telemetry_policy_decisions` | Adaptive GPS interval history | Unique event and checked state/interval |
| `processed_events` | Cross-module idempotency ledger | Event ID primary key + payload hash |
| `audit_events` | User/system traceability | Trace ID, actor, aggregate and details |
| `benchmark_results` | CARE vs baseline measurements | Explicit metrics and immutable configuration |
| `audit_export_manifests` | Tamper-evident compliance export packages | SHA-256 digest, export type, format, company/trip scoping |

## Freshness model

Every observation used as a fact carries:

- `recorded_at` or `observed_at`
- `received_at` where relevant
- `expires_at`
- `confidence`
- source/network state where relevant

The engine evaluates freshness at a supplied evaluation time. A stale record remains stored as evidence but is never silently treated as current.

## Determinism and auditability

- Telemetry, disruption, invalidation and telemetry-policy events use stable event IDs.
- `processed_events` prevents duplicate transitions and detects reused IDs with different payload hashes.
- Every context snapshot and plan evaluation stores an input hash.
- Evaluations persist hard violations, warnings, soft results, all utility terms, coefficients, confidence, freshness and explanation codes.
- Equal-utility tie-breaking uses the stable plan UUID string order documented in the rule catalogue.

## Selected-plan invariant

A partial unique index permits only one plan in `SELECTED` or `EXECUTING` state for a disruption. The service layer must select in a transaction and must ensure the plan evaluation is feasible before changing status.

## Spatial indexes

GiST indexes cover routes, providers, safe stops and telemetry locations. B-tree indexes cover trip/status, time series, provider availability, decision state and audit access patterns.

## Migration policy

Never edit a Flyway migration after it has been applied outside disposable local development. Add a new ordered migration and document its contract impact. Seed migration V4 is for the prototype demonstration; production deployment must separate or disable demo data.

## Notification channels and delivery logs

- `notification_channels`: Configures tenant webhooks, SMS, and web-push endpoints with encrypted/hashed HMAC-SHA256 secrets, minimum severity thresholds, and tenant isolation.
- `notification_delivery_logs`: Append-only audit log tracking every alert dispatch, suppression by severity, delivery outcome, signature header, and payload timestamp.

## Audit export manifests and compliance verification

- `audit_export_manifests`: Tamper-evident ledger of generated compliance export packages (FDA 21 CFR Part 11 / GDP). Records cryptographic SHA-256 digest, export type (cold-chain excursions, CARE decision chain, full dossier), format (JSON-LD, CSV, Printable HTML), generation timestamp, and generator identity.


