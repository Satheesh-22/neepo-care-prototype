# Start Here — Claude Code

> Historical workflow notice: the three Development Wave 1 branches described below have
> already been implemented and integrated. For current work, start with
> `../../HANDOFF_STATUS.md` and `../NEXT_DEVELOPMENT_STEP.md`. Retain this file as the record
> of the original parallel-development process.

The repository has completed pre-development Phases 1–10. It is not the finished application.

Claude Code requires an eligible subscription or API billing. If the team is using a free web assistant instead, follow `docs/assistant/FREE_WORKFLOW.md`; the same member prompts and approval gates still apply.

## First local setup

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1
```

Linux/macOS:

```bash
chmod +x scripts/*.sh backend/mvnw
./scripts/bootstrap.sh
```

Replace every placeholder in `.env`, then verify:

```bash
docker compose config
cd backend
./mvnw clean verify
```

## Git

If this ZIP is not inside a Git repository, use the matching `initialize-git` script. Add a private remote, push all branches and configure protection before parallel work.

## Open Claude Code

Open a terminal at the repository root, check out exactly one member branch, and run `claude`.

Then paste only the corresponding file:

- Member 1: `docs/claude/MEMBER_1_BACKEND_START.md`
- Member 2: `docs/claude/MEMBER_2_CARE_START.md`
- Member 3: `docs/claude/MEMBER_3_FRONTEND_START.md`

Claude must produce a Milestone 1 design and stop. Review that design before replying “Approved.”

## Approval reply

```text
Approved. Implement only the proposed Milestone 1.

Run all relevant validation, compilation and tests. Fix only failures caused by
this milestone. Report changed files, test results, verification commands,
contract status, limitations and the next suggested milestone.

Do not start Milestone 2. Do not commit, push or change branches.
```

## Integration

After review, a human commits and opens a pull request into `develop`. Run the end-to-end integration checks after every shared milestone merge. Never let separate Claude sessions silently alter the same contract.
