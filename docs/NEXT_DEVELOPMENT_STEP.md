# Next Development Step

The backend, CARE engine, PWA, local deployment, clean-database migration automation,
CI workflows, and release packaging are integrated and verified on `feature/full-stack-integration`.

## Current Status: Release Hardening & Acceptance Completed

The release hardening milestone is complete:

1. **Metrics Assertions**: Live stack validator (`scripts/validate-live-stack.mjs`) expanded to 23 checks with explicit assertions for telemetry ingestion count (INT-03), duplicate rejection count (INT-04), disruption detection count (INT-05), recovery plan count (INT-06), decision latency and tie-breaking (INT-09, INT-10), WebSocket delivery (INT-03), and offline queue replay and synchronization count (INT-15, INT-16).
2. **Clean-Database Flyway Migration Automation**: Scripts (`scripts/test-clean-database-migration.ps1` and `.sh`) provision an isolated PostgreSQL 17 container, execute Flyway migrations V1–V6 in order, verify 30 tables, 71 indexes, and safe demo seeds, without touching the development volume.
3. **Continuous Integration**: `.github/workflows/ci.yml` automates foundation verification, CARE engine tests, backend verify, frontend lint/tests/build, Compose config validation, clean DB migrations, and live acceptance runs.
4. **Release Packaging**: `scripts/package-release.ps1` and `.sh` produce secret-free, reproducible versioned archives (`dist/release/neepo-care-prototype-v1.0.0.zip`).

## Required Verification Commands

```powershell
# Foundation & Compose
python scripts/validate-foundation.py
docker compose config --quiet

# Clean DB Migration Test (isolated ephemeral container)
powershell -ExecutionPolicy Bypass -File .\scripts\test-clean-database-migration.ps1

# Live Stack Integration Gate (23 checks)
$env:NEEPO_TEST_PASSWORD = '<local demo password>'
node scripts/validate-live-stack.mjs

# Backend and CARE engine
cd backend
.\mvnw.cmd -B clean verify
cd ..\care-engine
..\backend\mvnw.cmd -B clean verify

# Frontend
cd ..\frontend
npm run lint
npm test -- --run
npm run build

# Release Packaging
cd ..
powershell -ExecutionPolicy Bypass -File .\scripts\package-release.ps1
```

## Next Milestone: Production Readiness & Deployment Hardening

For subsequent production milestones:

1. Secret management integration (e.g. AWS Secrets Manager / HashiCorp Vault / GCP Secret Manager).
2. Production TLS termination, rate-limiting, and ingress security policies.
3. PostGIS multi-region read replicas and automated point-in-time recovery (PITR) backups.
4. Telemetry load testing and horizontal autoscaling benchmarks.
