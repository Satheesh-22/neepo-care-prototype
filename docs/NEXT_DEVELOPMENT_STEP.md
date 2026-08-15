# Next Development Step

Pre-development Phases 1–10 are complete. The next action is Development Wave 1, performed on three separate feature branches. Each member must first submit a plan using the matching prompt and wait for approval.

## Member 1 — Backend platform

- Branch: `feature/backend-platform`
- Prompt: `docs/claude/MEMBER_1_BACKEND_START.md`
- Confirm the existing health, response-envelope, trace-ID, validation and isolated-test foundation.
- Close only remaining Backend Milestone 1 gaps.
- Do not begin authentication, fleet, trip or telemetry features until Milestone 1 is reviewed.
- Required check: `backend\mvnw.cmd --batch-mode clean verify` on Windows.

Completion evidence: all backend tests pass; the standard API/error contract remains aligned; no entity is exposed directly; no secret or schema mutation is introduced.

## Member 2 — CARE decision engine

- Branch: `feature/care-engine`
- Prompt: `docs/claude/MEMBER_2_CARE_START.md`
- Design immutable decision state and the context-freshness validator.
- Reuse the generated shared contracts.
- Keep Milestone 1 unit tests independent of PostgreSQL and the web layer.
- Do not implement plan scoring, recovery generation or re-planning yet.

Completion evidence: deterministic processing order is documented; boundary, expired, future-time and low-confidence cases are tested; hard facts never rely on stale evidence.

## Member 3 — Frontend/PWA

- Branch: `feature/frontend-pwa`
- Prompt: `docs/claude/MEMBER_3_FRONTEND_START.md`
- Plan the page map, component boundaries, central typed API adapter and contract-import strategy.
- Establish one approved React/TypeScript/Vite foundation only after plan approval.
- Keep planned APIs behind the same adapter interface as live APIs.
- Do not implement CARE logic in the frontend.

Completion evidence: routing and state boundaries are explicit; accessibility and offline behavior are planned; lint, type-check, unit-test and production-build commands are defined and pass after implementation.

## Integration order

1. All three members present Milestone 1 plans.
2. The team reviews shared interfaces before anyone edits a frozen contract.
3. Each member implements only the approved milestone on their branch.
4. Member 1's verified platform foundation is merged first.
5. CARE and frontend changes are rebased or merged from the reviewed `develop` baseline.
6. Run `scripts/validate-foundation.py` and all module checks before every pull request merge.

Do not run three assistants against the same folder. Use separate branches or separate Git worktrees and never share `.env`.
