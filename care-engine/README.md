# NEEPO-CARE Decision Engine

This Java 21 module owns deterministic CARE decision logic and depends only on the frozen generated contracts. It contains the immutable freshness projection, context-freshness gate and deterministic temperature disruption detectors. It has no Spring, web, persistence or network dependency.

## Freshness processing order

`DecisionState.from(TripContext)` creates a stable, key-sorted projection before validation:

1. Vehicle health, driver status, cargo condition and route condition are hard facts.
2. Route restrictions are hard facts, ordered by their stable observation key.
3. Provider availability and safe-stop status are advisory until a plan selects the resource; a later plan evaluator must promote selected resources to hard facts.
4. Only the latest telemetry observation is projected, ordered by `recordedAt` and then event ID. A future-dated latest observation remains unverified; the validator never falls back silently to older evidence.
5. Duplicate observation keys and invalid context hashes are rejected before evaluation.

For each observation, the validator then applies these checks in order:

1. Required metadata and confidence range
2. Timestamp order
3. Future observation/receipt time
4. Source-provided `UNVERIFIED` state
5. Expiry, where `evaluationTime == expiresAt` is expired
6. Minimum confirmed confidence
7. Aging-window classification

Expired or unverified hard facts set `hardFactsUsable=false`. Low-confidence hard facts become unverified. Low-confidence advisory evidence stays available only as explicitly warned evidence and cannot satisfy a later hard constraint unless it is revalidated after promotion.

## Determinism and integration boundary

- The caller supplies `evaluationTime`; the engine does not read the system clock.
- Assessments and freshness summaries are ordered lexicographically by stable key.
- Shared contract records are projected into immutable lists before evaluation.
- `CONTEXT_OBSERVATION_EXPIRED` and `LOW_CONTEXT_CONFIDENCE` are the only current frozen explanation codes used by this milestone.
- Missing, malformed and future observations are represented by internal `FreshnessIssue` values. A new public explanation code must be approved before those issues are exposed through an API.
- The backend integration must persist the exact shared `TripContext`, its SHA-256 `contextHash`, the returned minimum confidence, expired-data flag and freshness summary in `trip_context_snapshots` before plan evaluation begins.

The first backend integration boundary is now implemented by `TripContextSnapshotService`. The Spring Boot build compiles the CARE engine as a modular-monolith source boundary and the Docker build includes the same sources. Snapshot persistence is tenant-scoped, accepts only active trips, locks the owning trip row before allocating the next per-trip sequence, and stores the caller-supplied evaluation time, canonical JSON context, normalized context hash, minimum confidence, expired-required-data flag and network state in the existing V3 table. The full freshness assessment remains recoverable from the immutable context payload and is not duplicated into a schema column that does not exist.

This boundary intentionally does not expose a controller yet. Context assembly and a PostgreSQL migration-backed integration test belong to later backend–CARE milestones.

`RecoveryDecisionPersistenceService` now provides the next transactional boundary. It locks the active, tenant-scoped disruption tied to the exact context snapshot, allocates contiguous generation sequence values, and persists every selected, feasible and rejected candidate with its ordered steps and complete `PlanEvaluation`. It rejects inconsistent lifecycle states and explanations before writing. Because V3 has no separate decision-explanation table, the complete immutable `DecisionExplanation` is stored as structured JSON in a `RECOVERY_PLAN_SELECTED` audit event; plan-specific codes remain queryable in `plan_evaluations`. No migration was changed.

## Disruption detection

The current detector evaluates two rules from the frozen prototype scope:

- `VEHICLE-OVERHEAT-DETECT-001`: detects `VEHICLE_OVERHEATING` only when the fresh vehicle temperature is strictly greater than that vehicle's configured maximum. Equality is safe.
- `CARGO-TEMP-DETECT-001`: detects `CARGO_TEMPERATURE_DEVIATION` only when fresh cargo temperature is strictly outside its configured minimum/maximum range. Equality at either boundary is safe.

Rules use only their own required observations. For example, stale route evidence cannot suppress independently confirmed overheating, although it remains visible in the returned context-freshness result. Inactive trips and missing, stale, future or low-confidence rule inputs produce `INSUFFICIENT_CONTEXT` rather than a guessed result.

Detected disruptions are ordered by severity descending and then by the frozen `DisruptionType` enum order. Demo severities are explicit `DisruptionDetectionPolicy` values rather than hidden temperature deltas. `CARGO_TEMPERATURE_AT_RISK` is emitted for cargo deviation. The frozen catalogue has no overheating-detection explanation code, so overheating returns transparent threshold evidence and an empty public explanation-code list until a shared code is approved.

## Overheating recovery candidate generation

`RecoveryPlanGenerator` expands a confirmed overheating disruption into deterministic plan candidates before any feasibility or ranking decision:

- Each workshop, towing provider and substitute-vehicle provider in the immutable context produces its corresponding candidate.
- Each mobile-mechanic and safe-stop pair produces a mobile-repair candidate.
- One unbound emergency-escalation candidate is always retained, including when no normal resource exists.
- Candidate IDs are RFC 4122 version-5 UUIDs derived from the trip, disruption, context hash, plan type and bound resource IDs. Replaying the same snapshot therefore produces identical plans.
- Candidate order is the frozen `RecoveryPlanType` order, followed by provider and safe-stop UUID. Input list order cannot alter the result.
- Step timing uses explicit policy assumptions and the caller-provided context evaluation time. It does not silently substitute provider ETA for verified feasibility.

Generation deliberately retains stale, unavailable and apparently incompatible resources. Those facts are not ignored: the next hard-constraint stage promotes the selected resource observations to hard facts and records an explicit rejection. No plan is marked feasible, scored or selected in this stage.

## Independent hard-constraint evaluation

`HardConstraintEvaluator` applies all thirteen frozen catalogue rules to every normal candidate without short-circuiting. Each rule returns `SATISFIED`, `VIOLATED` or `NOT_APPLICABLE`, ordered exactly as the catalogue, with stable evidence and approved explanation codes. This makes simultaneous rejection causes visible instead of allowing an early failure to hide later safety problems.

Selected provider and safe-stop observations are promoted from advisory context to plan-critical hard facts. Expired, future, unverified, low-confidence or older-than-policy provider evidence cannot satisfy a plan. Exact height, wait, driver-time, temperature and deadline boundaries pass; only values above the configured limits reject.

Access restrictions use the existing seed-data naming convention `<RESOURCE_NAME>_ACCESS_<KIND>` to scope facility-specific restrictions, such as `WORKSHOP_A_ACCESS_HEIGHT`, to the matching provider or safe stop. Restrictions without `_ACCESS_` remain route-wide.

The unbound emergency-escalation candidate remains feasible even when normal context is unusable, without claiming that a provider or stop is confirmed. The evaluator returns an internal `HardConstraintPlanEvaluation`; it intentionally does not create the frozen shared `PlanEvaluation`, because that contract requires soft scores, coefficients and final utility. Those fields belong to the next scoring milestone.

The frozen context currently exposes current cargo temperature and refrigeration support, but no warming/cooling rate. `CARGO-TEMP-001` therefore enforces the fresh current boundary and confirmed refrigeration support without inventing a forecast; predictive drift needs a reviewed contract extension. Likewise, provider context currently exposes one aggregate `requiredResourceAvailable` flag, so this stage can reject unavailable equipment but cannot truthfully distinguish a missing spare part from other unavailable resources.

## Deterministic soft scoring

`RecoveryPlanScorer` evaluates soft preferences only after the hard gate. Feasible normal plans receive normalized 0–100 values for provider ETA, recovery duration, execution simplicity, provider reliability, observation confidence, remaining freshness and delivery margin. Explicit policy maps define benefit, risk and uncertainty weights plus `lambda` and `mu`; weights must total exactly one. Plan-type complexity points prevent a short towing template from being mistaken for low operational complexity. Provider reliability follows the existing 0–100 database and API contract, and an out-of-contract value receives no preference credit.

Scores use `BigDecimal`, `HALF_UP` and a fixed four-decimal output scale. The scorer stores every normalized component, aggregate score, coefficient and final `U(p) = B(p) - lambda * R(p) - mu * S(p)` in the frozen `PlanEvaluation`. Its SHA-256 input hash chains the hard-evaluation hash, evaluator version and complete scoring policy. Rejected plans receive zero soft contribution, so a hard violation can never become a penalty that scoring rescues.

Emergency escalation receives zero benefit and maximum risk/uncertainty. The selector treats it as a fallback that is eligible only when no normal plan is feasible. Per-plan route detour is not scored because the frozen context contains only the shared remaining route, not candidate-specific detour distance or duration; the engine does not invent that input.

## Deterministic plan selection

`RecoveryPlanSelector` selects only from normal plans that passed every hard constraint. It orders those candidates by final utility descending and resolves an exact numeric tie with the canonical plan UUID string ascending. The selected plan is copied to `SELECTED` with `selectedAt` equal to the shared caller-supplied evaluation time; the scored input records remain unchanged.

If no normal plan is feasible, the selector requires the single feasible `EMERGENCY_ESCALATION` candidate produced by generation. It records `NO_FEASIBLE_RECOVERY_PLAN` and `EMERGENCY_ESCALATION_SELECTED` instead of allowing the emergency candidate's deliberately low utility to compete with normal recovery plans. Missing or ambiguous fallback candidates are rejected as malformed input rather than guessed.

The frozen `DecisionExplanation` contains the selected plan, selection codes and the stable tie-break rule. Its rejected-plan collection contains only hard-rejected plans, ordered by UUID; a feasible plan that simply lost the utility comparison is not misrepresented as rejected.

## Idempotent invalidation and replanning trigger

`RecoveryPlanInvalidator` accepts a confirmed fresh change event for a `SELECTED` or `EXECUTING` plan. All nine frozen invalidation reasons map to their approved explanation codes: provider cancellation or expiry, ETA threshold breach, worsened vehicle condition, blocked route, cargo safety threat, insufficient driver time, unavailable resource and a higher-severity disruption. Provider-specific reasons cannot invalidate an unbound emergency plan.

The result is a copied `INVALIDATED` plan, the frozen `PlanInvalidation`, and a `ReplanningTrigger` that carries the same trip, disruption, invalidation and source-event identity. Invalidation IDs are RFC 4122 version-5 UUIDs derived from plan, event and reason, so replaying the same confirmed input produces the same outcome. Events older than plan selection are rejected and the original selected/executing plan remains immutable.

CARE does not publish events or start database cycles itself. `ReplanningLifecycleService` now persists the invalidation first, relies on the existing unique event constraint for delivery idempotency, transitions the active plan, creates one serialized replanning cycle, and records `PLAN_INVALIDATED` followed by `REPLANNING_STARTED` audit events in the same transaction. Exact event replays return the existing cycle; conflicting reuse is rejected.

## Automatic replanning orchestration

`RecoveryDecisionEngine` composes candidate generation, independent hard evaluation, soft scoring and deterministic selection without dropping candidates between stages. `RecoveryReplanner` accepts the persisted invalidation trigger, a newer immutable context containing the `INVALIDATED` current plan, and the still-active disruption. It verifies trip, disruption and time provenance before running the complete decision pipeline again.

Initial planning continues to require disruption evidence from the exact current context snapshot. Replanning has a separate generator entry point because the active disruption was legitimately detected from an older snapshot; the new snapshot supplies current providers, route, cargo, driver and vehicle evidence. A cancelled or stale provider is therefore regenerated for transparent rejection while an available alternative can be selected. Replaying the same trigger and snapshot returns the same decision, and an unchanged snapshot cannot select the exact invalidated plan instance again.

The backend transactionally persists each stage and assigns the unique cycle number under the disruption lock. Completion persists the entire replacement decision before marking the cycle `COMPLETED` and recording `REPLANNING_COMPLETED`; failures are timestamped terminal states with an audited reason. Completion and invalidation replays are idempotent. External event publication remains a later outbox/service-boundary milestone.

## Adaptive telemetry policy

`AdaptiveTelemetryPolicyEvaluator` returns one frozen `TelemetryPolicyState` and the configured upload interval for each immutable context. Demo defaults are 60 seconds for normal travel, 15 near a decision point, 5 during route deviation, 3 during breakdown/emergency and 120 once safely parked. `OFFLINE_BUFFERING` has no upload interval, matching the database constraint.

State precedence is explicit: offline buffering, confirmed safe parking, high/critical non-route emergency, route deviation, decision/freshness pressure, then normal travel. A selected or executing emergency plan also activates emergency monitoring; an invalidated plan activates near-decision monitoring. Expired, unverified or aging hard facts accelerate monitoring, while aging advisory resources that are unrelated to the current decision do not.

Because the frozen context has no direct near-decision or safely-parked observation, those facts arrive as a timestamped `TelemetryPolicySignal`; the engine never infers safe parking from a plan template alone. The decision uses the frozen policy-state name as its persistence explanation code because the shared explanation-code catalogue does not define separate telemetry codes. Scheduling, offline storage, transmission and idempotent persistence remain backend/device responsibilities.

`TelemetryPolicyPersistenceService` now owns the backend persistence boundary. It verifies the tenant and active trip, stores the exact policy decision, records one audit event, returns exact event replays idempotently and rejects event-ID reuse with different policy content. Device scheduling, buffering and transmission remain outside the backend boundary.

## Deterministic prototype benchmarks

`PrototypeBenchmarkRunner` compares the real CARE selection result with the required nearest-provider baseline. The baseline orders provider-bound normal plans by ETA and canonical plan UUID without consulting feasibility, intentionally exposing invalid or stale recommendations instead of quietly improving the baseline. Recovery success, invalid recommendations, deterministic recovery duration, rerouting, measured decision latency and stale-data decisions use the existing metrics schema.

`TelemetryBenchmarkRunner` evaluates the same ordered operational windows using CARE's adaptive intervals and a configured fixed interval. Offline windows generate no upload for either strategy, partial intervals round up to one transmission, overlapping windows are rejected and bandwidth is derived from an explicit payload size. Input order cannot change the result.

Every `PrototypeBenchmarkReport` requires scenario code, dataset version, environment, commit hash, measurement time, configuration and limitations. Decision latency is a measured input; unit-test execution time is never presented as product latency. This milestone implements the two baselines required by the prototype demo—nearest provider and fixed telemetry. Static weighted ranking, no-freshness validation and no-replanning ablations remain later experiment-suite extensions.

`BenchmarkPersistenceService` persists one row per strategy with deterministic RFC 4122 version-5 identity derived from the complete report provenance, configuration and metrics. Environment, commit hash and limitations are retained in structured configuration JSON, new results are audited and exact report imports are idempotent without changing V3.

## Verify

```powershell
cd care-engine
mvn.cmd --batch-mode clean test
```
