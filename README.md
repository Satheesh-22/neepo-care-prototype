# NEEPO-CARE Prototype

NEEPO-CARE is an explainable logistics disruption-recovery prototype. The system receives simulated trip telemetry, detects operational disruptions, evaluates recovery plans against hard and soft constraints, selects a deterministic plan, monitors it, and re-plans when an accepted plan becomes invalid.

## Current status

This AI-development-ready archive completes pre-development Phases 1–10:

- Environment and Git workflow checks
- Frozen prototype scope, enums and rule vocabulary
- Complete OpenAPI, CARE and WebSocket contracts
- Shared Java and TypeScript models
- Full physical database design and ERD
- PostgreSQL/PostGIS Docker foundation
- Four ordered Flyway migrations, including repeatable demo data
- Versioned user-management API with UUID identifiers
- Standard success and error responses
- Isolated backend tests using an in-memory test profile
- Member-specific assistant prompts and milestone commands

The CARE engine and React PWA intentionally remain future milestones. They should be implemented independently only after the shared contracts are reviewed.

## Structure

```text
backend/       Spring Boot backend
contracts/     API, CARE, events, enums and Java/TypeScript contracts
database/      Physical schema, ERD and demo-data documentation
docs/          Scope, API, rules, testing, demo and patent records
frontend/      Reserved for the React/PWA milestone
docker/        Docker-related support files
scripts/       Bootstrap, prerequisite, Git and validation scripts
.claude/       Reusable Claude Code milestone commands
```

## Requirements

- Java 21
- Node.js 22
- Python 3.11+
- Docker Desktop with Docker Compose
- Git
- Claude Code with an eligible account, or the free file-handoff workflow

## Direct Claude Code setup

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1
```

Linux/macOS:

```bash
chmod +x scripts/*.sh backend/mvnw
./scripts/bootstrap.sh
```

Then read `docs/claude/START_HERE.md`. If this extracted folder is not already in Git, use the included `initialize-git` script before starting parallel member sessions.

## Development without paid Claude Code

Claude Code is optional. For a free web-assistant workflow, read `docs/assistant/FREE_WORKFLOW.md`. It preserves the same branch ownership, plan-before-code gate, validation requirements and three-member prompts while a human controls Git and local commands.

The immediate three-member work sequence and integration gates are in `docs/NEXT_DEVELOPMENT_STEP.md`.

## Start infrastructure and backend

Create the local environment file:

```bash
cp .env.example .env
```

Replace the development passwords in `.env`, then run:

```bash
docker compose config
docker compose up --build -d postgres redis backend
docker compose ps
```

Health endpoint:

```text
http://localhost:8080/actuator/health
```

## Run backend tests

Linux/macOS:

```bash
cd backend
./mvnw test
```

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd test
```

Tests use the `test` profile and do not require a running PostgreSQL instance.

## Implemented API

```text
POST   /api/v1/users
GET    /api/v1/users?page=0&size=20
GET    /api/v1/users/{userId}
GET    /api/v1/users/by-email?email=user@example.com
DELETE /api/v1/users/{userId}
```

The complete planned prototype contract is in `contracts/openapi.yaml`. Endpoints marked as future milestones must not be presented as already implemented.

## Branch workflow

- `main`: stable demonstration releases
- `develop`: reviewed integration branch
- `feature/backend-platform`: Member 1
- `feature/care-engine`: Member 2
- `feature/frontend-pwa`: Member 3

Start each member only from the matching feature branch and matching prompt under `docs/claude/`. Claude must propose Milestone 1 and wait for approval before editing.
