# Free AI Development Workflow

This workflow is for team members who do not have a Claude Code subscription or API billing. The AI assistant handles an uploaded, sanitized project copy; a human remains responsible for Git, local execution, review and merging.

## Non-negotiable controls

- Keep `.env`, credentials, private keys, database volumes and build outputs out of every upload.
- Use exactly one feature branch and one approved milestone per handoff.
- Paste the matching member prompt from `docs/claude/` with the upload.
- Require a plan before code changes.
- Review the returned diff and run validation locally before committing.
- Never let two members change shared contracts independently.
- Never replace an applied Flyway migration; add a new ordered migration after team approval.

## One-time baseline

From the repository root, confirm the verified baseline is committed on `develop`. Create the three feature branches described in `docs/setup/GIT_WORKFLOW.md`. Each member works only on the branch assigned in `CLAUDE.md`.

## Handoff cycle

1. Check out the member's feature branch and confirm a clean working tree.
2. Create a sanitized ZIP outside the project directory.
3. Upload that ZIP to the assistant with the matching member prompt.
4. Ask for inspection and a Milestone 1 plan only.
5. Review the plan as a team and approve it explicitly.
6. Ask the assistant to implement only the approved milestone and return a corrected ZIP plus changed-file and test reports.
7. Compare the returned files, run local validation and commit only reviewed changes.
8. Open a pull request into `develop`; do not merge shared-contract changes without all three owners.

## Windows sanitized ZIP command

Run from Command Prompt while the feature branch is checked out:

```cmd
cd /d D:\Projects
tar.exe -a -c -f neepo-care-handoff.zip --exclude=neepo-care-prototype/.git --exclude=neepo-care-prototype/.env --exclude=neepo-care-prototype/backend/target --exclude=neepo-care-prototype/node_modules --exclude=neepo-care-prototype/frontend/node_modules --exclude=neepo-care-prototype/dist neepo-care-prototype
```

Create a new ZIP name for each reviewed milestone so an older handoff remains recoverable.

## Initial prompt for each member

Attach the sanitized ZIP, then paste the complete matching file:

- Member 1: `docs/claude/MEMBER_1_BACKEND_START.md`
- Member 2: `docs/claude/MEMBER_2_CARE_START.md`
- Member 3: `docs/claude/MEMBER_3_FRONTEND_START.md`

Add this final instruction:

```text
This is a sanitized ZIP without Git metadata. Treat the branch named in the member prompt as the declared working branch. Inspect and plan only. Do not edit until I explicitly approve the plan. When implementation is approved, preserve the single project root, exclude secrets and build outputs, and return a corrected ZIP with a complete changed-file and validation report.
```

## Approval prompt

```text
Approved. Implement only the proposed Milestone 1. Preserve all frozen contracts unless the approved plan explicitly requires a reviewed contract change. Run every validation available in your environment, clearly separate passed checks from environment-blocked checks, and return the corrected ZIP. Report changed files, tests, verification commands, limitations and the next suggested milestone. Do not start Milestone 2.
```

## Required local verification

From the project root:

```cmd
python scripts\validate-foundation.py
cd backend
mvnw.cmd --batch-mode clean verify
cd ..
docker compose config --quiet
docker compose up --build -d postgres redis backend
docker compose ps
curl.exe -i http://localhost:8080/actuator/health
```

Frontend milestones must additionally run the repository's lint, type-check, test and production-build commands once the frontend package has been created.
