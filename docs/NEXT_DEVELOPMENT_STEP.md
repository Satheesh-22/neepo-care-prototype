# Next Development Step

The backend, CARE engine, PWA, local deployment, clean-database migration automation,
CI workflows, and release packaging are integrated and verified on `feature/full-stack-integration`.

## Current Status: Web-Page Style & Data Visualization Completed

The web-page style, visual hierarchy, dashboard operations telemetry, interactive Leaflet route map, CARE decision explainability comparisons, responsive layouts, accessibility, and demo presentation modes have been implemented and verified:

1. **Design System Refinement**: `theme.ts` & `styles.css` with oceanic navy/teal contrast, micro-animations, accessible focus rings, and reduced-motion support.
2. **Operations Dashboard Visualization**: `DashboardPage.tsx` & `MetricCard.tsx` with active trips, disruption cards, decision latency, evidence freshness, offline queue status, and sparklines.
3. **Telemetry Visualization**: `TelemetryGauges.tsx` & `DriverTripPage.tsx` with engine overheating limits, cold-chain compliance bands (2°C–8°C), speed gauges, and adaptive policy interval displays.
4. **Live Route & Incident Map**: `TripMap.tsx` with route polylines, detour tracks, vehicle heading, origin depot, incident point (106°C), safe stop hub, partner mechanic, and center-on-vehicle controls.
5. **CARE Decision Visualization**: `RecoveryPlanComparison.tsx` & `TripWorkspacePage.tsx` with multi-dimensional scoring ($U = \text{Benefit} - \lambda\text{Risk} - \mu\text{Uncertainty}$), constraint compliance badges, decision journey timeline, and cryptographic hash evidence.
6. **Accessible & Responsive Polish**: 320px–1920px responsiveness, keyboard navigation, and WCAG AA contrast.
7. **Demo Presentation Mode**: `DemoModeBar.tsx` interactive controller for operator demonstrations and guided scenario testing.

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
