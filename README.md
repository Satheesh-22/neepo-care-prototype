# NEEPO-CARE Prototype

NEEPO-CARE is an explainable logistics disruption-recovery prototype. It receives trip
telemetry, detects operational disruptions, evaluates recovery plans against hard and soft
constraints, selects a deterministic plan, monitors it and re-plans when the accepted plan
becomes invalid.

## Current status

The end-to-end local application is implemented on `feature/full-stack-integration`:

- Java 21 and Spring Boot backend with JWT authentication, RBAC and tenant isolation
- Fleet, cargo, provider, trip, telemetry and disruption APIs
- Deterministic CARE context validation, recovery generation, scoring, selection,
  invalidation, re-planning and benchmark metrics
- PostgreSQL 17 with PostGIS and six ordered Flyway migrations
- Redis-backed local service boundary
- React, TypeScript and Vite PWA for dispatcher and driver workflows
- Adaptive telemetry policy, offline telemetry queue and explicit synchronization
- Authenticated one-use WebSocket tickets and post-commit telemetry updates
- Docker Compose deployment with health-checked database, cache, backend and frontend
- Clean-database Flyway migration automation on isolated ephemeral containers
- Reproducible, secret-free release packaging automation
- Continuous Integration workflow covering backend, CARE engine, frontend, contracts, database migration and live acceptance

See `HANDOFF_STATUS.md` and `docs/testing/INTEGRATION_TEST_PLAN.md` for verified results and test mappings.

## Repository structure

```text
backend/       Spring Boot API, persistence, security and WebSocket delivery
care-engine/   Deterministic CARE domain logic
contracts/     OpenAPI, event and generated Java/TypeScript contracts
database/      Physical schema, ERD and demo-data documentation
docs/          Scope, architecture, rules, testing and handoff records
frontend/      React/TypeScript PWA and Nginx gateway configuration
scripts/       Bootstrap, validation, migration tests, packaging and Git utilities
```

## Requirements

- Java 21
- Node.js 22 or newer
- Python 3.11 or newer
- Docker Desktop with Docker Compose
- Git

## Local setup

Create a local environment file and replace every placeholder. The file is ignored by Git
and must never be committed.

```powershell
Copy-Item .env.example .env
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Local endpoints:

- PWA and API gateway: `http://localhost:3000`
- Frontend health: `http://localhost:3000/health`
- Backend: `http://localhost:8080`
- Backend health: `http://localhost:8080/actuator/health`

The repeatable seed data contains local-only dispatcher and driver accounts. Their setup is
defined in migrations V4–V6; never reuse those credentials outside local development.

## Validation

Backend and CARE engine:

```powershell
cd backend
.\mvnw.cmd -B clean verify
cd ..\care-engine
..\backend\mvnw.cmd -B clean verify
```

Frontend:

```powershell
cd frontend
npm ci
npm run lint
npm test -- --run
npm run build
```

Repository, database migration and live deployment:

```powershell
python scripts/validate-foundation.py
docker compose config --quiet
powershell -ExecutionPolicy Bypass -File .\scripts\test-clean-database-migration.ps1
$env:NEEPO_TEST_PASSWORD = '<local demo password>'
node scripts/validate-live-stack.mjs
```

Release packaging:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-release.ps1
```

`validate-live-stack.mjs` runs 23 checks covering health and authentication, tenant-aware
trip provisioning, assignment and start, telemetry ingestion and duplicate count assertions (INT-03, INT-04),
authenticated WebSocket event delivery (INT-03), offline telemetry queue replay and synchronization count (INT-15, INT-16),
automatic overheating detection count (INT-05), deterministic recovery generation and plan count (INT-06),
hard-constraint evidence (including expired provider rejection INT-08), decision explanation and latency assertions with
deterministic tie-breaking (INT-09, INT-10), adaptive emergency and safely-parked telemetry policy (INT-13, INT-14),
invalidation and automatic re-planning (INT-11, INT-12). Optional endpoint, account and seeded-ID overrides use the
`NEEPO_GATEWAY_URL`, `NEEPO_BACKEND_URL`, `NEEPO_DISPATCHER_EMAIL`,
`NEEPO_DRIVER_EMAIL`, `NEEPO_TEST_TRIP_ID`, `NEEPO_TEST_VEHICLE_ID` and
`NEEPO_TEST_ROUTE_ID` environment variables.

The verified run passed 118 backend tests, 117 CARE-engine tests, 7 frontend tests,
frontend lint and production build, the foundation validator, clean-database migration test,
Compose validation, release packaging verification and all 23 repeatable live-stack checks.

## API and events

The versioned API is served under `/api/v1`. Its contract covers authentication, users,
vehicles, drivers, cargo, providers, trips, telemetry, disruptions, recovery operations,
decision explanations and metrics:

- OpenAPI: `contracts/openapi.yaml`
- WebSocket contract: `contracts/websocket-events.yaml`
- API conventions: `docs/api/API_CONTRACT.md`

The WebSocket endpoint is `/ws`. Clients first obtain a short-lived, one-use ticket from
`POST /api/v1/events/ticket`; access tokens are not placed in the WebSocket URL.

## Scope and safety

The prototype scope is frozen in `docs/PROTOTYPE_SCOPE.md`. It intentionally excludes
machine learning, real vehicle hardware, payments, Kafka, Kubernetes and separately deployed
microservices. CARE decisions remain deterministic and explainable.

- Do not edit an applied Flyway migration; add a new ordered migration.
- Do not delete the PostgreSQL volume simply to rerun development.
- Do not expose `.env`, JWTs or WebSocket tickets in source, output or logs.
- Keep frontend behavior behind the typed API adapter; do not duplicate CARE rules in the UI.

## Branches

- `main`: stable demonstration releases
- `develop`: reviewed integration baseline
- `feature/backend-platform`: backend foundation history
- `feature/care-engine`: CARE engine history
- `feature/frontend-pwa`: frontend history
- `feature/full-stack-integration`: current integrated application

Historical assistant prompts remain under `docs/claude/`; they describe the original
parallel development workflow and are not the current next-step plan.
