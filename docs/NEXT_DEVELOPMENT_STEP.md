# Next Development Step

The backend, CARE engine, PWA, local deployment, clean-database migration automation,
CI workflows, and release packaging are integrated and verified on `feature/full-stack-integration`.

## Current Status: Milestones 1 through 6 Completed

Milestones 1, 2, 3, 4, 5, and 6 of the v1.1 roadmap have been implemented, integrated, and verified:

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

4. **Fleet Reliability & Cost-Avoidance Analytics (Milestone 4)**:
   - Created Flyway migration `V10__create_fleet_analytics_domain.sql` defining `fleet_analytics_snapshots` with JSONB metrics and pre-seeded 7d, 30d, 90d baseline data.
   - Implemented `FleetAnalyticsService` and `AnalyticsController` with `GET /api/v1/analytics/fleet` supporting dynamic interval selection (`7d`, `30d`, `90d`), MTTR, driver compliance, preserved cargo valuation, telematics root causes, and CARE strategy efficiency.
   - Integrated `FleetAnalyticsPage.tsx` with interactive timeframe controls, executive KPI cards, root-cause distribution bars, incident progression trend, and solver strategy table.
   - Added `/analytics` route with RoleGuard (`ADMIN`, `DISPATCHER`) and navigation link in AppShell.
   - Verified test suites: 162/162 backend tests, 117/117 care-engine tests, 33/33 frontend tests, production PWA build, and foundation validator (V1–V10).

5. **Hardware Gateway Fleet Pilot & Offline Resilience (Milestone 5)**:
   - Created Flyway migration `V11__create_hardware_gateway_domain.sql` defining `hardware_gateways` and `gateway_sync_logs` tables with composite indexes.
   - Implemented `HardwareGatewayService` and `HardwareGatewayController` with `GET/POST /api/v1/gateways`, `GET /api/v1/gateways/{id}/config`, `POST /api/v1/gateways/{id}/sync`, and `POST /api/v1/gateways/{id}/heartbeat`.
   - Supported HMAC-SHA256 signature verification, SPI flash batch telemetry ingestion with idempotent deduplication (`ON CONFLICT (event_id) DO NOTHING`), and dynamic vehicle safety/cold-chain envelope resolution.
   - Integrated `HardwareGatewayPage.tsx` with fleet KPI summary cards, gateways table with search/filtering, registration dialog with vehicle assignment, and live OTA diagnostics & offline flash replay simulator.
   - Added `/gateways` route with RoleGuard (`ADMIN`, `DISPATCHER`) and navigation link in AppShell.
   - Verified test suites: 170/170 backend tests, 117/117 care-engine tests, 37/37 frontend tests, ESLint clean (0 errors), clean Vite PWA build, and foundation validator (V1–V11).

6. **Multi-Tenant Enterprise Organization & SSO Integration (Milestone 6)**:
   - Created Flyway migration `V12__create_enterprise_sso_domain.sql` defining `company_sso_configs` and `sso_audit_logs` with unique domain constraints, cascade rules, and seed configuration for demo company.
   - Implemented `SsoService`, `SsoAuthController`, `SsoAdminController`, and password login SSO enforcement in `AuthService` (`checkSsoEnforcementForPasswordLogin`).
   - Implemented endpoints `GET /api/v1/auth/sso/discover`, `POST /api/v1/auth/sso/callback`, `GET/PUT /api/v1/admin/sso`, `GET /api/v1/admin/sso/audit-logs`, and `POST /api/v1/admin/sso/test`.
   - Built frontend `EnterpriseSsoPage.tsx` with KPI overview, IdP configuration form (OIDC / SAML 2.0), real-time IdP latency diagnostics, and tamper-evident SSO audit ledger.
   - Integrated enterprise domain discovery and SSO enforcement into `LoginPage.tsx` (blocking password entry when SSO is enforced, offering quick federated sign-in).
   - Added `/admin/sso` route protected with `RoleGuard(["ADMIN"])` and navigation link in `AppShell.tsx`.
   - Verified test suites: 184/184 backend tests, 117/117 care-engine tests, 41/41 frontend tests, ESLint clean (0 errors), clean Vite PWA build, and foundation validator (V1–V12).

## Next Suggested Milestone

1. **v1.1 Milestone 7: Hierarchical Multi-Org Fleet Partitioning & Automated SCIM 2.0 User Provisioning**:
   - Cross-tenant sub-organizations, depot boundaries, and hierarchical fleet partitioning (`sub_organizations`, `depots`).
   - Automated SCIM 2.0 user & group synchronization (`/scim/v2/Users`, `/scim/v2/Groups`) for enterprise identity providers (Okta, Azure AD / Microsoft Entra ID).
   - Granular RBAC scoping by organizational unit and depot assignment.

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
