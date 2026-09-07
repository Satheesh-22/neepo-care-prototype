# NEEPO-CARE External Secret Management & Vault Integration

This document defines the production secret lifecycle, external provider integration (HashiCorp Vault / AWS Secrets Manager), dynamic synchronization via Kubernetes External Secrets Operator (`ESO`), key rotation, and zero-leakage security boundaries.

---

## 1. Secret Architecture

```mermaid
flowchart LR
    subgraph SecretVault["Central Enterprise Secret Store"]
        Vault["HashiCorp Vault / AWS Secrets Manager"]
    end

    subgraph K8sControl["Kubernetes Cluster (neepo-prod)"]
        ESO["External Secrets Operator (ESO Controller)"] -->|Poll / Sync (1h Interval)| Vault
        ESO -->|Generate / Refresh| K8sSecret["K8s Secret (neepo-backend-secrets)"]
        K8sSecret -->|Env Injection (TmpFS)| BackendPod["Spring Boot Backend Pods"]
    end
```

### Managed Secret Inventory
| Secret Identifier | Remote Vault Path | Target Variable | Rotation Period | Purpose |
|---|---|---|---|---|
| `jwt_secret` | `secret/data/neepo/production/backend` | `JWT_SECRET` | 90 days | HMAC-SHA256 JWT signing key ($\ge 256$ bits) |
| `password` (DB) | `secret/data/neepo/production/database` | `DATABASE_PASSWORD` | 60 days | PostgreSQL application user (`neepo_app`) password |
| `password` (Redis)| `secret/data/neepo/production/redis` | `REDIS_PASSWORD` | 90 days | Redis cluster `AUTH` password |
| `tls.crt / tls.key`| `cert-manager / Let's Encrypt` | `neepo-tls-cert` | 60 days | Edge TLS certificate for `care.neepo.io` |
| `hmac_key` | `secret/data/neepo/production/hardware` | `HARDWARE_DEVICE_HMAC_KEY` | 180 days | Shared secret for physical gateway HMAC validation |

---

## 2. Vault Policy & ServiceAccount RBAC

### Vault Policy (`neepo-backend-policy.hcl`)
```hcl
path "secret/data/neepo/production/*" {
  capabilities = ["read"]
}
path "secret/metadata/neepo/production/*" {
  capabilities = ["read", "list"]
}
```

### Kubernetes ServiceAccount Token Authentication
The backend service account (`neepo-backend-sa`) authenticates to Vault via the Kubernetes Auth Method using the projected pod service account token.

---

## 3. Secret Rotation Lifecycle & Zero-Downtime Rollover

1. **Database Credentials**:
   - Rotate database user password in PostgreSQL using Vault dynamic database credentials.
   - Update remote secret path. ESO reconciles K8s Secret within 1 hour or immediately upon webhook trigger.
   - Trigger rolling update of `neepo-backend` pods: `kubectl rollout restart deployment/neepo-backend -n neepo-prod`.
2. **JWT Signing Key**:
   - Issue new primary key while retaining old key as secondary in `NimbusJwtDecoder` for token verification.
   - After grace period (60 minutes, exceeding token TTL), revoke the previous key.

---

## 4. Zero-Leakage Verification Safeguards

1. **No Source File Secrets**: Pre-commit and CI hooks run `scripts/validate-foundation.py` scanning for forbidden credential patterns.
2. **No Image Baking**: Dockerfiles use multistage builds with non-root runtime users; secrets are never passed as `ARG` or copied in image layers.
3. **No Artifact Inclusion**: `scripts/package-release.ps1` explicitly purges `.env`, `*.key`, `*.pem`, and test credentials from the release archive.
4. **Log Redaction**: Passwords, authorization tokens, and WebSocket tickets are stripped by Logback encoder and Nginx access logs.
