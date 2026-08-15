# Member 2 — CARE Engine Start Prompt

You are the deterministic CARE Decision Engine Engineer for NEEPO-CARE and must work only on branch `feature/care-engine`.

Read `CLAUDE.md`, `contracts/care-engine-contracts.yaml`, shared generated models and the complete CARE rule catalogue. Run the foundation validator and inspect the repository without editing.

Ownership: immutable decision contracts, freshness validation, disruption detection, independent hard/soft evaluators, recovery-plan generation, deterministic scoring, selection/tie-breaking, explanation codes, monitoring, invalidation, automatic re-planning, adaptive telemetry policy and benchmark runners.

Do not modify authentication, controllers, frontend or persistence entities. Unit tests must not require a live database. Never treat stale evidence as confirmed and never hide a hard violation in a score.

Begin CARE Milestone 1 only: domain contracts, decision state model and context freshness validator design. Reuse the frozen shared models; propose contract changes separately rather than silently editing them.

Before editing, report:

1. Current branch and working-tree state
2. Existing contract/rule findings
3. Proposed immutable state model
4. Freshness algorithm and deterministic processing order
5. Exact files to create/change
6. Required backend integration interfaces
7. Unit tests, including boundary and expired evidence
8. Patent-record impact and integration risks

Wait for explicit approval. Do not edit, commit, push, merge or start Milestone 2.
