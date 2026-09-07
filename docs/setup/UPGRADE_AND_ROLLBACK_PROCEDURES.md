# NEEPO-CARE Production Upgrade & Rollback Procedures

This document defines standard operating procedures for zero-downtime rolling upgrades, forward database migrations, rollback recovery, and edge client resynchronization.

---

## 1. Zero-Downtime Deployment Strategy

NEEPO-CARE employs a **Rolling Update** deployment pattern with Kubernetes:
- `maxSurge: 25%` (new pods spin up and become healthy before old pods terminate)
- `maxUnavailable: 0` (capacity is never reduced below 100% during deployment)

```
[ Ingress Controller ]
       |
       +---> [ Pod v1.0.0 (Running) ] 
       +---> [ Pod v1.0.0 (Running) ]
       +---> [ Pod v1.0.1 (Readiness Probe Passing...) ] -> Takes Traffic
```

### Readiness & Liveness Probes:
- **Liveness**: `/actuator/health/liveness` (checks JVM and internal deadlock state)
- **Readiness**: `/actuator/health/readiness` (checks PostgreSQL and Redis connectivity before receiving traffic)

---

## 2. Database Migration Rules (Flyway)

1. **Forward Compatibility Only**:
   - Every database migration must be non-breaking and backwards-compatible with running $N-1$ versions.
   - Column additions must have default values or be nullable.
   - Column removals require a multi-stage migration (Deprecate $\rightarrow$ Stop Reading/Writing $\rightarrow$ Drop).
2. **Applied Migrations are Immutable**:
   - Never alter applied migrations (`V1` through `V6`).
   - Fixes or changes must be added as `V7__...`.

---

## 3. Rollback Procedure

If a deployed version exhibits anomalies (e.g., error rate $>1\%$ or p95 solver latency $>250\text{ms}$):

### Step 1: Trigger Kubernetes Rollback
```bash
# Rollback backend
kubectl rollout undo deployment/neepo-backend -n neepo-care

# Rollback frontend
kubectl rollout undo deployment/neepo-frontend -n neepo-care
```

### Step 2: Verify Rollback Health
```bash
kubectl rollout status deployment/neepo-backend -n neepo-care
curl -f http://<INGRESS_HOST>/actuator/health
```

### Step 3: Edge & Gateway Resync
- Hardware gateways and web frontends automatically reconnect with exponential backoff.
- Buffered offline flash records are preserved and flushed once connectivity is restored.

---

## 4. Disaster Recovery & PITR
If a critical data anomaly occurs requiring database restoration:
Execute the automated restore drill procedure:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\drill-database-restore.ps1
```
RTO Target: $< 30$ seconds.
RPO Target: $< 5$ minutes.
