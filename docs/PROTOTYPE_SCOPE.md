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

## Scope control

Shared contracts and this scope require team review before change. A milestone must not claim a planned endpoint or mock workflow is implemented.
