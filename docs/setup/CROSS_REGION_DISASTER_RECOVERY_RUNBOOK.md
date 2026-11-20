# NEEPO-CARE Cross-Region Disaster Recovery Runbook
## Automated Failover, Health Probing & Standby Promotion Procedures
### v1.1 Milestone 9 Enterprise Operating Standard

---

## 1. Executive Summary & Recovery Objectives

NEEPO-CARE is a deterministic cold-chain logistics disruption-recovery platform. In catastrophic regional failure scenarios (e.g., primary cloud datacenter loss, fiber severance, or major regional compute degradation), the platform guarantees:

| Metric | Target SLA | Verification Method |
|---|---|---|
| **Recovery Point Objective (RPO)** | $< 5\text{ seconds}$ | Asynchronous WAL streaming with continuous LSN synchronization and replication slot checkpoints. |
| **Recovery Time Objective (RTO)** | $< 30\text{ seconds}$ | Automated standby promotion via `pg_ctl promote` and Geo-DNS ingress traffic shift. |
| **Cold-Chain Telemetry Ingestion Data Loss** | Zero ($0\text{ records}$) | On-device SPI NOR Flash edge buffer replay (`Winbond 32MB`) with server-side idempotent deduplication (`ON CONFLICT (event_id) DO NOTHING`). |
| **Solver State Determinism** | 100% | Stateless CARE engine solvers instantly rebuild disruption contexts upon database promotion. |

---

## 2. Multi-Region Replication Architecture

```text
       +----------------------------------------------------------------+
       |             Global Traffic Manager / Geo-DNS (Anycast)          |
       +-------------------------------+--------------------------------+
                                       |
                   +-------------------+-------------------+
                   | (Active: 100%)                        | (Hot Standby: 0%)
                   v                                       v
     +---------------------------+           +---------------------------+
     |   Primary Region (East)   |           |  Secondary Region (West)  |
     |                           |           |                           |
     | [Ingress Gateway]         |           | [Ingress Gateway]         |
     |      |                    |           |      |                    |
     | [Spring Boot Backend]     |           | [Spring Boot Standby Pod] |
     |      |                    |           |      |                    |
     | [PostGIS 17 Primary] -----+-- WAL ----> [PostGIS 17 Hot Standby]  |
     |  (wal_level=replica)      |  Replication(hot_standby=on)          |
     |                           |  Slot     |                           |
     | [Redis Primary Cache] ----+-- Active -> [Redis Replica Cache]     |
     |                           |   Passive |                           |
     +---------------------------+           +---------------------------+
```

### 2.1 Database Replication Specifications
- **Primary Configuration**:
  - `wal_level = replica`
  - `max_wal_senders = 10`
  - `max_replication_slots = 10`
  - `archive_mode = on`
  - Physical replication slot: `standby_region_slot`
- **Standby Configuration**:
  - `hot_standby = on`
  - `hot_standby_feedback = on`
  - Standby signal: `standby.signal` created in PostgreSQL `$PGDATA`
  - Primary connection: `primary_conninfo = 'host=postgres-primary-repl.neepo.internal port=5432 user=replicator password=...'`

---

## 3. Automated Health Probing & Failure Detection

The continuous probe suite (`scripts/probe-cross-region-health.mjs`) evaluates health telemetry across 5 vital dimensions every 5 seconds:

1. **Ingress Gateway Latency**: Asserts $< 50\text{ms}$ HTTP response time.
2. **Cold-Chain Sensor Ingestion**: Transmits synthetic OBD-II engine and cargo thermal packets.
3. **CARE Deterministic Solver**: Evaluates overheating disruption detection and plan scoring.
4. **Replication Lag Calculation**:
   $$\text{Replication Lag (Bytes)} = \text{pg\_current\_wal\_lsn}() - \text{pg\_last\_wal\_replay\_lsn}()$$
   $$\text{Lag (Seconds)} = \text{EXTRACT(EPOCH FROM (now() - pg\_last\_xact\_replay\_timestamp()))} < 5.0\text{s}$$
5. **Geo-DNS Routing Liveness**: Checks synthetic traffic distribution and health endpoints.

A failover event is declared when **3 consecutive synthetic probes fail** across a 15-second observation window.

---

## 4. Emergency Failover Runbook (Step-by-Step)

### Phase 1: Primary Outage Isolation & Fencing
To prevent split-brain anomalies, terminate write access to the primary region before promoting the standby:

```bash
# Fence primary cluster traffic
kubectl scale deployment/backend --replicas=0 -n neepo-staging --context=primary-cluster
# Revoke primary DB ingress network policy
kubectl apply -f deploy/dr/network-policy-fence.yaml --context=primary-cluster
```

### Phase 2: Standby Database Promotion
Promote the secondary PostgreSQL replica to read-write master:

```bash
# Execute promotion on standby container
kubectl exec -it postgres-standby-region-0 -n neepo-staging --context=secondary-cluster -- \
  pg_ctl promote -D /var/lib/postgresql/data

# Verify promotion status (returns false when master)
kubectl exec -it postgres-standby-region-0 -n neepo-staging --context=secondary-cluster -- \
  psql -U neepo_user -d neepo_care -c "SELECT pg_is_in_recovery();"
```

### Phase 3: Traffic Migration & Global Ingress Shift
Update Global Load Balancer / Route53 weights to route 100% of ingress to the secondary region:

```bash
# Shift Geo-DNS traffic to secondary region
aws route53 change-resource-record-sets --hosted-zone-id Z123456789 --change-batch file://deploy/dr/route53-failover-shift.json
```

### Phase 4: Edge Sensor Gateway Flush & Replay
Trigger edge gateway offline flash buffer replay:
- Edge gateways detect connection restoration to the secondary endpoint.
- Replay buffered observations from SPI Flash.
- Server-side deduplication preserves strict order and prevents duplicate trip metrics.

---

## 5. Verification Drills

Disaster recovery readiness is verified via automated drill suites:

```powershell
# Run cross-region continuous health probe
node scripts/probe-cross-region-health.mjs

# Run automated RPO/RTO disaster recovery drill (Windows PowerShell)
powershell -ExecutionPolicy Bypass -File .\scripts\drill-disaster-recovery.ps1

# Run automated RPO/RTO disaster recovery drill (Linux/macOS Bash)
./scripts/drill-disaster-recovery.sh
```

