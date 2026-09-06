# CARE Rule Catalogue

## Processing order

1. Build an immutable trip-context snapshot.
2. Classify required observations as fresh, aging, expired or unverified.
3. Detect all simultaneous disruptions in deterministic severity/type order.
4. Generate applicable recovery-plan templates.
5. Apply every hard constraint independently.
6. Reject plans with one or more hard violations.
7. Evaluate soft preferences only for feasible plans.
8. Calculate `U(p) = B(p) - lambda * R(p) - mu * S(p)`.
9. Select the greatest utility; break exact ties by stable plan UUID string order.
10. Persist evaluation inputs, results, freshness, confidence and explanation codes.

Hard violations are never converted into score penalties.

## Shared thresholds

Thresholds are configuration, not magic constants. Initial demonstration defaults:

| Setting | Default |
|---|---:|
| Provider availability maximum age | 10 minutes |
| Minimum confirmed confidence | 0.80 |
| Normal telemetry interval | 60 seconds |
| Near-decision interval | 15 seconds |
| Route-deviation interval | 5 seconds |
| Emergency interval | 3 seconds |
| Safely parked interval | 120 seconds |

## Hard rules

### VEHICLE-COMPAT-001 — Vehicle compatibility

- Inputs: vehicle type/dimensions/weight, provider supported vehicle types, substitute specification
- Condition: the plan requires a provider or substitute that cannot accept the vehicle or cargo configuration
- Result: reject
- Codes: `VEHICLE_TYPE_UNSUPPORTED`
- Tests: exact match passes; unsupported class rejects; missing capability is not treated as support

### ROUTE-DIM-001 — Height, width and weight restrictions

- Inputs: vehicle height/width/gross weight and restrictions for every required route/facility segment
- Condition: any vehicle dimension or gross weight exceeds a hard limit
- Result: reject
- Codes: `ROAD_HEIGHT_LIMIT_EXCEEDED`, `ROAD_WIDTH_LIMIT_EXCEEDED`, `ROAD_WEIGHT_LIMIT_EXCEEDED`
- Tests: below/equal/above each limit; multiple simultaneous violations

### CARGO-SAFE-001 — Cargo safety

- Inputs: cargo type, handling constraints, equipment, transfer compatibility
- Condition: a recovery step cannot preserve a mandatory handling or containment requirement
- Result: reject
- Codes: `CARGO_REQUIREMENT_UNSUPPORTED`
- Tests: compatible transfer passes; incompatible vehicle rejects; missing evidence rejects

### CARGO-TEMP-001 — Temperature limit

- Inputs: current temperature, permitted range, warming/cooling rate, predicted wait and equipment state
- Condition: predicted temperature leaves the permitted range before recovery stabilizes it
- Result: reject
- Codes: `CARGO_TEMPERATURE_AT_RISK`
- Tests: boundary remains valid; predicted breach rejects; stale temperature rejects through freshness first

### PROVIDER-CAP-001 — Provider capability

- Inputs: required capabilities and confirmed provider capabilities
- Condition: at least one required capability is absent
- Result: reject
- Codes: `PROVIDER_CAPABILITY_MISSING`
- Tests: all capabilities pass; one missing rejects; unknown is not equivalent to present

### PROVIDER-FRESH-001 — Provider availability freshness

- Inputs: availability value, observed time, expiry time, confidence, evaluation time
- Condition: unavailable, expired, or too old for the configured maximum age
- Result: reject or require re-verification before selection
- Codes: `PROVIDER_UNAVAILABLE`, `PROVIDER_AVAILABILITY_EXPIRED`
- Tests: fresh available passes; expired rejects; future timestamp rejects as invalid evidence

### RESOURCE-001 — Required equipment and parts

- Inputs: plan requirements and confirmed resource inventory
- Condition: required equipment or part is unavailable for the relevant time window
- Result: reject
- Codes: `REQUIRED_EQUIPMENT_UNAVAILABLE`, `REQUIRED_PART_UNAVAILABLE`
- Tests: all present passes; unavailable/expired inventory rejects

### DRIVER-HOURS-001 — Driving and rest limits

- Inputs: remaining legal driving time, travel/wait duration, required rest and revised departure
- Condition: executing the plan would exceed remaining permitted driving time before a valid rest
- Result: reject
- Codes: `DRIVER_HOURS_INSUFFICIENT`
- Tests: enough time passes; exact boundary passes; over boundary rejects; rest plan recalculates remaining time

### SAFE-STOP-001 — Safe stopping

- Inputs: disruption severity, vehicle mobility and verified safe-stop data
- Condition: the plan requires waiting/stopping where safety is not confirmed
- Result: reject
- Codes: `STOP_LOCATION_UNSAFE`
- Tests: verified parking passes; roadside unknown rejects; emergency escalation remains available

### WAIT-001 — Maximum waiting time

- Inputs: provider ETA, queue time, cargo/vehicle safe waiting limit
- Condition: expected wait exceeds the minimum applicable maximum
- Result: reject
- Codes: `WAITING_TIME_EXCEEDED`
- Tests: within/equal/above limit; expired ETA rejects via freshness

### DEADLINE-001 — Delivery deadline

- Inputs: route time, recovery time, revised ETA and hard deadline
- Condition: revised arrival exceeds a contractually hard deadline
- Result: reject
- Codes: `DELIVERY_DEADLINE_EXCEEDED`
- Tests: before/equal/after deadline; time zones normalized to UTC

### ROUTE-BLOCK-001 — Blocked route

- Inputs: all required plan segments and fresh route conditions
- Condition: any required segment is confirmed blocked and no evaluated detour exists
- Result: reject
- Codes: `ROUTE_BLOCKED`
- Tests: open passes; blocked rejects; stale route condition is unverified, not open

### CONTEXT-FRESH-001 — Network and data uncertainty

- Inputs: required observations, observed/received/expiry timestamps, confidence and network state
- Condition: any plan-critical observation is expired or unverified
- Result: reject normal selection or force safe emergency handling
- Codes: `CONTEXT_OBSERVATION_EXPIRED`
- Tests: fresh passes; expired rejects; low confidence warns only when the datum is not required for a hard fact

## Soft preferences

Soft preferences never rescue a rejected plan. For feasible plans they contribute normalized values:

- Lower provider ETA
- Lower detour distance/time
- Lower recovery duration
- Lower execution complexity
- Higher provider reliability
- Higher observation confidence and remaining freshness margin
- Better delivery margin

Warnings include `LOW_CONTEXT_CONFIDENCE`, `ROUTE_DETOUR_HIGH` and `PROVIDER_ETA_HIGH`.

## Utility configuration

- `B(p)`: normalized operational benefit, 0–100
- `R(p)`: normalized execution/safety risk, 0–100
- `S(p)`: normalized uncertainty/staleness penalty, 0–100
- `lambda`: configurable risk coefficient
- `mu`: configurable uncertainty coefficient

All terms and coefficients are stored with the evaluation. Rounding occurs only after the final calculation using a documented fixed scale.

## Invalidation rules

An executing plan is invalidated idempotently when a fresh event confirms:

- Provider cancellation or expired availability
- Accepted ETA threshold exceeded
- Vehicle condition worsened
- Required route blocked
- Cargo safety threatened
- Driver time insufficient
- Required resource unavailable
- Higher-severity disruption detected

The invalidation is persisted before `REPLANNING_STARTED` is published. Repeated delivery of the same event ID must return the existing outcome without creating a second cycle.

## Emergency fallback

If no normal plan is feasible, generate `EMERGENCY_ESCALATION`, record `NO_FEASIBLE_RECOVERY_PLAN`, choose the safest verified stop or emergency instruction available, and never invent provider confirmation.
