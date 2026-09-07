# NEEPO-CARE v1.1 Product Roadmap & Specification

This document details the functional specifications, architectural requirements, and acceptance criteria for the upcoming NEEPO-CARE **v1.1** release.

---

## 1. Executive Summary & Product Vision

NEEPO-CARE v1.0 established a robust, deterministic disruption-recovery platform with sub-second constraint solving, real-time telemetry streaming, and automated replanning. 
**Version 1.1** focuses on enterprise fleet scale, actionable operational analytics, multi-channel alerting, deep compliance reporting, and physical telematics pilot readiness.

---

## 2. Prioritized Feature Roadmap Matrix

| Priority | Epic Name | Target Audience | Primary Business Value | Complexity |
|---|---|---|---|---|
| **P1** | High-Resolution Telemetry History & Breadcrumb Replay | Dispatchers, Compliance Officers | Historical audit and accident reconstruction | Medium |
| **P1** | Multi-Channel Real-Time Alerting (Push / SMS / Webhooks)| Drivers, Dispatchers, On-Call SRE | Sub-second incident notification delivery | Medium |
| **P1** | Cryptographic Audit & Compliance Evidence Exports | Fleet Regulators, Cargo Insurers | Tamper-evident proof of cold-chain and vehicle safety | Low |
| **P2** | Fleet Reliability & Cost-Avoidance Analytics | Fleet Managers, Executives | Quantifiable ROI and recovery efficiency metrics | Medium |
| **P2** | Role-Tailored Operational Dashboards | Fleet Admins, Dispatchers, Drivers | Optimized UX and reduced cognitive load | Medium |
| **P2** | Physical Hardware Pilot Protocol (mTLS + Secure Element) | Hardware Partners, Field Engineers | Laboratory-to-vehicle certified telematics pilot | High |
| **P3** | Multi-Tenant Enterprise Administration & SSO | Enterprise IT, Security Teams | SAML 2.0 / OIDC federation and role quotas | High |
| **P3** | Accessibility (WCAG 2.1 AA) & Multi-Language i18n | Global Fleets, Cross-Border Drivers | Inclusive UX and multi-lingual driver interfaces | Low |
| **P3** | Automated Cross-Region Disaster Recovery | SRE / Infrastructure Team | Zero-RPO multi-region hot standby replication | High |

---

## 3. Detailed Epic Specifications & Acceptance Criteria

### Epic 1: Telemetry History & Interactive Route Replay
- **Specification**: Enable dispatchers to scrub through historical telemetry timelines for any completed or active trip with time-series charts (Speed, Coolant Temp, Cargo Temp) aligned to a Leaflet/MapLibre route trace.
- **Acceptance Criteria**:
  - `GET /api/v1/trips/{tripId}/telemetry/history?from={iso}&to={iso}` returns time-bounded observations in $< 150\text{ms}$.
  - Frontend scrubber renders smooth playback with play/pause/step controls at $1\times, 2\times, 5\times$ speed.
  - Telemetry anomalies (overheating, sensor dropouts) are visually highlighted as annotated pins on the timeline.

### Epic 2: Multi-Channel Real-Time Alerting & Notification Engine
- **Specification**: Configurable notification policies delivering real-time disruption events across WebPush, SMS (Twilio/AWS SNS), and external Webhooks (Slack/PagerDuty).
- **Acceptance Criteria**:
  - When an overheating or refrigeration disruption occurs, notifications trigger within $< 500\text{ms}$.
  - Dispatchers can configure per-severity routing rules (e.g. Critical $\rightarrow$ SMS + WebPush; Warning $\rightarrow$ Dashboard only).
  - Outgoing webhook payloads contain HMAC-SHA256 signatures for recipient validation.

### Epic 3: Cryptographic Audit & Compliance Exports
- **Specification**: Generate tamper-evident, cryptographically signed audit reports for cold-chain excursions, CARE plan acceptance history, and driver acknowledgment timestamps.
- **Acceptance Criteria**:
  - Exports available in CSV, JSON-LD, and printable PDF formats with SHA-256 integrity digests.
  - Includes immutable snapshot hashes for every CARE solver execution.
  - Compliant with FDA 21 CFR Part 11 and GDP (Good Distribution Practice) requirements for pharmaceutical/perishable cargo.

### Epic 4: Fleet Analytics & Recovery Cost-Avoidance Dashboard
- **Specification**: Aggregate fleet-wide disruption metrics, Mean Time to Recovery (MTTR), driver compliance rates, and estimated cost avoidance across chosen timeframes (7d, 30d, 90d).
- **Acceptance Criteria**:
  - Computes saved transit hours and avoided cargo spoilage value based on prompt CARE intervention.
  - Visual breakdown of top disruption root causes (Coolant failure, Traffic, Ambient temperature spikes).
  - Fast aggregation using PostgreSQL materialized views refreshed incrementally.

### Epic 5: Controlled Physical Hardware Gateway Pilot
- **Specification**: Extend the bench-test gateway emulator to support hardware-in-the-loop (HIL) testing with certified automotive MCU boards (ESP32-S3 / STM32F4 + Microchip ATECC608A secure element).
- **Acceptance Criteria**:
  - Hardware units authenticate using client TLS certificates signed by internal Device CA.
  - Power-fail safe SPI NOR flash ring buffer retains $> 10,000$ observations during total power disconnects.
  - Zero packet loss across simulated cellular handoffs (2G/3G/4G/5G).

---

## 4. Architectural Prerequisites & Database Migration Strategy

1. **Database Schema Additions (Flyway `V7`)**:
   - `notification_channels` and `notification_logs` tables.
   - `audit_export_manifests` table.
   - Materialized view `mv_fleet_monthly_analytics` with concurrent refresh triggers.
2. **Backward Compatibility Guarantee**:
   - Zero breaking changes to `V1`–`V6` entities or existing OpenAPI `/api/v1/` endpoints.
   - All v1.1 extensions follow additive REST resource semantics.
