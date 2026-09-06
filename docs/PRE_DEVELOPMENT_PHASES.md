# Pre-development Phases 1–10

This document is the hand-off gate for Claude Code. “Complete” means the repository contains the required artifact; machine-specific installation and GitHub protection settings still require the developer to execute the supplied checks.

| Phase | Deliverable | Repository evidence | Status |
|---:|---|---|---|
| 1 | Development environment definition | `docs/setup/DEVELOPMENT_ENVIRONMENT.md`, prerequisite scripts | Complete |
| 2 | Repository structure and branch workflow | `docs/setup/GIT_WORKFLOW.md`, initialization scripts | Complete |
| 3 | Frozen prototype scope | `docs/PROTOTYPE_SCOPE.md` | Complete |
| 4 | Shared vocabulary and enums | `contracts/domain-enums.yaml` | Complete |
| 5 | Final logical/physical database design | `database/schema-design.md`, `database/erd.mmd`, Flyway V1–V3 | Complete |
| 6 | Frozen API contract | `contracts/openapi.yaml`, `docs/api/API_CONTRACT.md` | Complete |
| 7 | Shared Java and TypeScript contracts | `contracts/generated/java`, `contracts/generated/typescript` | Complete |
| 8 | Safe environment configuration | `.env.example`, `.gitignore`, application profiles | Complete |
| 9 | Docker Compose foundation | `docker-compose.yml`, backend Dockerfile | Complete |
| 10 | Repeatable demonstration data | Flyway V4 and `database/seed/DEMO_DATA.md` | Complete |

## Ready-to-code gate

Before any member asks Claude Code to edit a feature branch:

1. Run the prerequisite check.
2. Initialize Git and create the agreed branches if this ZIP was extracted outside an existing clone.
3. Run the foundation validator.
4. Review and approve the scope, enums, database design, OpenAPI and rule catalogue as a team.
5. Create `.env` from `.env.example` and replace placeholders.
6. Run backend tests and Docker checks on Java 21.
7. Commit the pre-development baseline to `develop`.
8. Start only the first milestone for the member’s branch.

No file in this package means that a later feature is implemented. Contracts marked `planned` define integration targets for Claude Code milestones.
