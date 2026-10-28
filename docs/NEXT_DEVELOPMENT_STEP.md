# Next Development Step

The backend, CARE engine, PWA, local deployment, clean-database migration automation,
CI workflows, and release packaging are integrated and verified on `feature/full-stack-integration`.

## Current Status: Milestone 1, Milestone 2 & Milestone 3 (v1.1 P1) Completed

Milestones 1, 2, and 3 of the v1.1 roadmap have been implemented, integrated, and verified:

1. **High-Resolution Telemetry History & Route Replay (Milestone 1)**:
   - Added `GET /api/v1/trips/{tripId}/telemetry/history` with optional ISO-8601 bounds, limit clamping (1–2000), tenant isolation, and RBAC.
   - Added `findHistory` to `TelemetryJdbcRepository` utilizing the `idx_telemetry_trip_recorded` composite index.
   - Integrated `TelemetryReplayScrubber.tsx` with Play/Pause, step controls, $1\times, 2\times, 5\times$ speed multipliers, and thermal/cold-chain alert badges.
   - Dynamic Leaflet vehicle tracking and historical breadcrumb trail polylines.

2. **Multi-Channel Real-Time Alerting & Webhooks Engine (Milestone 2)**:
   - Created Flyway migration `V8__create_alerting_and_notification_domain.sql` defining `notification_channels` and `notification_delivery_logs`.
   - Implemented `NotificationAlertService` with HMAC-SHA256 signature verification (`X-Neepo-Signature`), severity hierarchy suppression (`LOW` < `MEDIUM` < `HIGH` < `CRITICAL`), secret masking, and test ping dispatching.
   - Implemented `NotificationController` with RBAC (`ADMIN`, `DISPATCHER`) and multi-tenant isolation.
   - Integrated `DisruptionNotificationEventListener` via Spring event publishing for post-commit disruption alert distribution.
   - Created `NotificationSettingsModal.tsx` in frontend with channel management, test ping triggers, HMAC signature validation, and delivery audit log inspection.
   - Verified 150/150 backend tests and 27/27 frontend tests passing cleanly.

3. **Cryptographic Audit & Compliance Evidence Exports (Milestone 3)**:
   - Created Flyway migration `V9__create_compliance_and_audit_manifest_domain.sql` defining `audit_export_manifests`.
   - Implemented `ComplianceExportService` producing multi-format outputs (`JSON_LD`, `CSV`, `PRINTABLE_HTML`), SHA-256 tamper-evident digest computation, regulatory context formatting (FDA 21 CFR Part 11 & EU GDP Guidelines), and bit-level tamper verification.
   - Implemented `ComplianceExportController` with `POST /api/v1/compliance/exports/trip/{tripId}`, `GET /api/v1/compliance/exports`, `GET /api/v1/compliance/exports/{manifestId}`, and `POST /api/v1/compliance/exports/{manifestId}/verify` (with `X-Neepo-Digest` response header).
   - Created `ComplianceExportModal.tsx` in frontend with Export Generator, bit-level Cryptographic Tamper Verifier, and Audit Ledger views, integrated into `TripWorkspacePage.tsx`.
   - Verified test suites: 157/157 backend tests, 117/117 care-engine tests, 31/31 frontend tests, and clean foundation validator (V1–V9).

## Next Suggested Milestone

1. **v1.1 Milestone 4 (P2): Fleet Reliability & Cost-Avoidance Analytics**: Materialized aggregate views and executive metrics tracking Mean Time to Recovery (MTTR), driver compliance rates, and estimated cost avoidance across chosen intervals.
2. **v1.1 Milestone 5 (P2): Hardware Gateway Fleet Pilot & Offline Resilience**: End-to-end telemetry ingestion from cellular hardware gateways with dead-zone NOR flash buffer replay.

## Required Verification & Benchmark Commands

```powershell
# Foundation & Compose
python scripts/validate-foundation.py
docker compose config --quiet

# Clean DB Migration Test (isolated ephemeral container)
powershell -ExecutionPolicy Bypass -File .\scripts\test-clean-database-migration.ps1

# Disposable Database Restore Drill (isolated ephemeral container)
powershell -ExecutionPolicy Bypass -File .\scripts\drill-database-restore.ps1

# Upgrade & Rollback Validation Suite
powershell -ExecutionPolicy Bypass -File .\scripts\test-upgrade-rollback.ps1

# Hardware Sensor Gateway Bench Prototype
node scripts/bench-hardware-gateway.mjs

# Live Stack Integration Gate (23 checks)
$env:NEEPO_TEST_PASSWORD = '<local demo password>'
node scripts/validate-live-stack.mjs

# Live Stack Performance & Scaling Benchmark
node scripts/benchmark-live-stack.mjs

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

## Next Production Steps

1. Configure cloud provider IAM credentials and cluster access (AWS EKS / GCP GKE) when authorized.
2. Link External Secrets Operator to target cloud secret store (AWS Secrets Manager / Vault).
3. Connect staging CI/CD pipeline triggers and schedule weekly restore drills.
