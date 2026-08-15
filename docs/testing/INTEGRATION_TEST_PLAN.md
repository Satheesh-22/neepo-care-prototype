# NEEPO-CARE Integration Test Plan

| ID | Scenario | Expected result |
|---|---|---|
| INT-01 | Dispatcher creates a trip | Trip persists and appears in the dashboard |
| INT-02 | Driver starts assigned trip | Status becomes `IN_PROGRESS` |
| INT-03 | Fresh GPS telemetry arrives | Context and map update once |
| INT-04 | The same event ID is resent | Existing result is returned; no duplicate state |
| INT-05 | Engine temperature reaches 112°C | `VEHICLE_OVERHEATING` is detected |
| INT-06 | Three plans are generated | Ordered steps and evaluations persist |
| INT-07 | Nearest workshop has a height restriction | Plan is rejected with `ROAD_HEIGHT_LIMIT_EXCEEDED` |
| INT-08 | Provider availability is expired | Provider is not treated as confirmed |
| INT-09 | Feasible plans are scored | Greatest deterministic utility is selected |
| INT-10 | Equal utilities occur | Stable plan-ID tie-break is reproducible |
| INT-11 | Selected provider cancels | Plan becomes `INVALIDATED` exactly once |
| INT-12 | Invalidation is persisted | Re-planning begins and a replacement is selected |
| INT-13 | Emergency begins | Telemetry interval changes to configured emergency value |
| INT-14 | Vehicle is safely parked | Telemetry interval changes to 120 seconds |
| INT-15 | Driver loses network | Events remain queued with original IDs/timestamps |
| INT-16 | Network returns | Events synchronize in order and only acknowledged items leave the queue |
| INT-17 | WebSocket reconnects | UI resumes without duplicate timeline entries |
| INT-18 | No normal plan is feasible | Emergency escalation is produced and explained |

Each executed test must record preconditions, inputs, API response, database state, WebSocket event, UI state, actual result, pass/fail and evidence.

## Foundation verification

Before domain integration begins:

- `./mvnw test` passes on Java 21
- `docker compose config` succeeds with a populated `.env`
- PostgreSQL, Redis and backend health checks become healthy
- Flyway applies the identity migration to a new database
- A second backend startup validates without modifying the schema
- Secrets do not appear in source, logs or the frontend bundle
