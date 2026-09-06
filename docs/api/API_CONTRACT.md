# NEEPO-CARE API Contract

The machine-readable source of truth is `contracts/openapi.yaml`.

Internal CARE input/output invariants are in `contracts/care-engine-contracts.yaml`. Live event envelopes are in `contracts/websocket-events.yaml`.

## Conventions

- Base path: `/api/v1`
- JSON fields: camelCase
- Identifiers: UUID
- Timestamps: ISO-8601 UTC
- Authentication: `Authorization: Bearer <JWT>` after the authentication milestone
- List endpoints: zero-based `page` and bounded `size`
- Entities are never returned directly from persistence
- Every non-empty successful response contains `success`, `data`, `timestamp` and `traceId`
- Every error contains a stable code, safe message, field errors, timestamp and trace ID

## Success example

```json
{
  "success": true,
  "data": {
    "id": "f505c9d3-d4e0-40fc-8ec8-bd5411e7727f",
    "name": "Demo Dispatcher",
    "email": "dispatcher@neepo.local",
    "phone": "+919999999999",
    "role": "DISPATCHER",
    "createdAt": "2026-08-13T10:30:00Z",
    "updatedAt": "2026-08-13T10:30:00Z"
  },
  "timestamp": "2026-08-13T10:30:00Z",
  "traceId": "4aaf6af7-b6cc-476f-9314-2656ef566c08"
}
```

## Error example

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid values.",
    "fieldErrors": [
      {
        "field": "email",
        "message": "Email must be valid"
      }
    ]
  },
  "timestamp": "2026-08-13T10:30:00Z",
  "traceId": "4aaf6af7-b6cc-476f-9314-2656ef566c08"
}
```

## Idempotency

Telemetry and offline driver events carry a client-generated `eventId`. Repeated submission of the same event must not create duplicate state transitions. The server returns the original event identity and indicates whether the event was already known.

## Contract-change policy

Contract changes require:

1. OpenAPI update
2. Java and TypeScript model review
3. Database impact review
4. Backward-compatibility decision
5. Tests for both producer and consumer
6. Approval by all module owners

Operations marked `x-implementation-status: planned` are not yet live.

## Planned endpoint groups

- Authentication and users
- Vehicles and drivers
- Cargo
- Trips, assignment and lifecycle
- Provider search and time-bounded availability
- Idempotent telemetry ingestion
- Explicit/manual disruption reporting
- Recovery generation, selection and invalidation
- Decision explanations
- CARE/baseline metrics

## WebSocket event names

`TELEMETRY_UPDATED`, `DISRUPTION_DETECTED`, `PLANS_GENERATED`, `PLAN_SELECTED`, `PLAN_INVALIDATED`, `REPLANNING_STARTED`, `REPLANNING_COMPLETED` and `TRIP_STATUS_CHANGED`.
