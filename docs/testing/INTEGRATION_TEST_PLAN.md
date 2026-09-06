# NEEPO-CARE Integration Test Plan

| ID | Scenario | Expected result | Live Check Evidence |
|---|---|---|---|
| INT-01 | Dispatcher creates a trip | Trip persists and appears in the dashboard | `acceptance trip resource provisioning` |
| INT-02 | Driver starts assigned trip | Status becomes `IN_PROGRESS` | `acceptance trip assignment and start` |
| INT-03 | Fresh GPS telemetry arrives | Context and map update once | `telemetry ingestion count assertion` & `post-commit telemetry WebSocket delivery` |
| INT-04 | The same event ID is resent | Existing result is returned; no duplicate state | `duplicate telemetry event count assertion` |
| INT-05 | Engine temperature reaches 112°C | `VEHICLE_OVERHEATING` is detected | `automatic overheating disruption detection count` |
| INT-06 | Three plans are generated | Ordered steps and evaluations persist | `automatic recovery plan generation count` |
| INT-07 | Nearest workshop has a height restriction | Plan is rejected with `ROAD_HEIGHT_LIMIT_EXCEEDED` | `hard-constraint rejection evidence` |
| INT-08 | Provider availability is expired | Provider is not treated as confirmed | `expired provider availability rejection (INT-08)` |
| INT-09 | Feasible plans are scored | Greatest deterministic utility is selected | `deterministic tie-break rule and decision explanation with latency assertion` |
| INT-10 | Equal utilities occur | Stable plan-ID tie-break is reproducible | `deterministic tie-break rule and decision explanation with latency assertion` |
| INT-11 | Selected provider cancels | Plan becomes `INVALIDATED` exactly once | `invalidation and automatic re-planning` |
| INT-12 | Invalidation is persisted | Re-planning begins and a replacement is selected | `invalidation and automatic re-planning` |
| INT-13 | Emergency begins | Telemetry interval changes to configured emergency value | `adaptive emergency telemetry policy` |
| INT-14 | Vehicle is safely parked | Telemetry interval changes to 120 seconds | `adaptive safely parked telemetry policy` |
| INT-15 | Driver loses network | Events remain queued with original IDs/timestamps | `offline telemetry queue replay and synchronization count` |
| INT-16 | Network returns | Events synchronize in order and only acknowledged items leave the queue | `offline telemetry queue replay and synchronization count` |
| INT-17 | WebSocket reconnects | UI resumes without duplicate timeline entries | `one-use WebSocket ticket authentication` & `consumed ticket rejection` |
| INT-18 | No normal plan is feasible | Emergency escalation is produced and explained | Handled via CARE evaluator hard-rejection rules & fallback plan generator |

Each executed test records preconditions, inputs, API response, database state, WebSocket event, UI state, actual result, pass/fail and evidence.

## Current execution status

The repeatable `scripts/validate-live-stack.mjs` run passes 23 live stack checks.
It provisions an isolated tenant trip, assigns and starts it, proves telemetry ingestion and ticket idempotency,
asserts offline queue replay and sequential synchronization (INT-15, INT-16), automatically captures CARE context,
detects 112°C overheating (INT-05), persists at least three evaluated plans (INT-06), proves hard-constraint rejection
(including route dimension and expired provider availability rejection INT-08), retrieves the selected decision explanation
with decision latency and deterministic tie-break validation (INT-09, INT-10), evaluates adaptive emergency and safely-parked
telemetry policy (INT-13, INT-14), invalidates the selected plan, and verifies automatic replacement selection (INT-11, INT-12).

## Foundation verification

- `./mvnw test` passes on Java 21 (118 backend tests, 117 CARE-engine tests)
- `docker compose config` succeeds with a populated `.env`
- PostgreSQL, Redis and backend health checks become healthy
- Flyway applies all ordered migrations V1–V6 on a clean ephemeral database (`scripts/test-clean-database-migration.ps1` / `.sh`)
- Zero secrets or tokens in logs, source, or packaged release ZIP (`scripts/package-release.ps1` / `.sh`)
