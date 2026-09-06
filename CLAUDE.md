# NEEPO-CARE Development Instructions

## Mission

NEEPO-CARE is a deterministic logistics disruption-recovery prototype. It must detect a disruption, generate several recovery plans, reject plans that violate hard constraints, rank feasible plans, explain the decision, monitor the selected plan, invalidate it when conditions change, and re-plan automatically.

The frozen demonstration scenario is documented in `docs/PROTOTYPE_SCOPE.md`.

## Source of truth

Read these files before proposing or changing code:

1. `docs/PRE_DEVELOPMENT_PHASES.md`
2. `docs/PROTOTYPE_SCOPE.md`
3. `contracts/domain-enums.yaml`
4. `contracts/openapi.yaml`
5. `contracts/care-engine-contracts.yaml`
6. `contracts/websocket-events.yaml`
7. `contracts/explanation-codes.yaml`
8. `database/schema-design.md`
9. `docs/rules/CARE_RULE_CATALOGUE.md`

Shared field names, enum values, event names, explanation codes, database relationships, and API payloads must not be changed without explicit team approval.

## Technology baseline

- Java 21 and Spring Boot
- PostgreSQL 17 with PostGIS
- Flyway-managed schema migrations
- Redis for transient coordination where a later milestone requires it
- React, TypeScript and Vite for the frontend
- UUID identifiers
- UTC ISO-8601 timestamps
- REST endpoints under `/api/v1`
- WebSocket events defined by the shared contract

Do not introduce MySQL, Kafka, Kubernetes, machine learning, paid map services, a native mobile application, or real IoT hardware in the prototype.

## Team ownership

- Member 1 (`feature/backend-platform`): backend platform, persistence, APIs, authentication, telemetry ingestion, WebSocket publication and deployment foundation.
- Member 2 (`feature/care-engine`): freshness, detection, constraints, recovery-plan generation, deterministic scoring, explanations, invalidation, re-planning and benchmarks.
- Member 3 (`feature/frontend-pwa`): React application, driver PWA, dispatcher UI, map, simulator, offline queue, WebSocket client and frontend tests.

Do not modify another member's owned module unless the team has approved the integration change.

## First Claude Code action

At the start of a new Claude Code session:

1. Read this file and every source-of-truth file above completely.
2. Run `python scripts/validate-foundation.py`.
3. Inspect the current branch and working tree.
4. Read the matching member prompt in `docs/claude/`.
5. Report the current milestone plan only.
6. Do not edit until the user explicitly approves that plan.

If the current branch does not match the member prompt, stop and report it. Do not switch branches, merge, commit, push or open a pull request without explicit instruction.

## Milestone workflow

Work on only one approved milestone at a time.

Before editing, report:

1. Existing-code findings
2. Proposed algorithm or implementation
3. Exact files to create or change
4. Required database and API contracts
5. Tests
6. Integration risks

Wait for approval. After approval, implement only that milestone, run relevant checks, fix implementation-related failures, list changed files, explain the logic, state limitations, and suggest—but do not start—the next milestone.

## Backend rules

- Never expose JPA entities directly from controllers.
- Use UUIDs and UTC timestamps.
- Use Flyway migrations; never use Hibernate `ddl-auto=update`.
- Return the standard response and error shapes in `contracts/openapi.yaml`.
- Validate all external input.
- Keep secrets in environment variables and never commit real credentials.
- Make event processing idempotent by stable event ID.
- Do not report an operation as successful until persistence or acknowledgement succeeds.

## CARE engine rules

- Validate freshness before treating observations as facts.
- Hard violations reject a plan and must never be hidden inside a score.
- Score only feasible plans using the deterministic model `U(p) = B(p) - lambda * R(p) - mu * S(p)`.
- Use stable enums/constants for statuses and explanation codes.
- Identical inputs and configuration must produce identical results.
- Document processing order, tie-breaking rules and assumptions.

## Frontend rules

- Use a central typed API client matching `contracts/openapi.yaml`.
- Keep mocks behind the same interface as live APIs.
- Do not scatter hardcoded demonstration data through components.
- Do not use colour alone to communicate safety or status.
- Preserve offline event IDs and original timestamps; do not claim synchronization before acknowledgement.
- Do not bypass production authentication.

## Required verification

Backend milestones must run compilation and tests. Frontend milestones must run linting, type checking, tests and a production build. Contract or database changes must be reviewed by all three members before merge.

Never silently weaken a test, hard constraint, validation rule or security check to make a build pass.
