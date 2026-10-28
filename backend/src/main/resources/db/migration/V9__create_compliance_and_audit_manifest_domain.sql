-- V9: Cryptographic audit and compliance evidence export manifest domain

CREATE TABLE IF NOT EXISTS audit_export_manifests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    trip_id UUID REFERENCES trips (id) ON DELETE SET NULL,
    export_type VARCHAR(50) NOT NULL,
    format VARCHAR(30) NOT NULL,
    sha256_digest VARCHAR(64) NOT NULL,
    record_count INT NOT NULL DEFAULT 0,
    generated_by_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_export_manifests_type CHECK (export_type IN ('COLD_CHAIN_EXCURSION', 'CARE_DECISION_CHAIN', 'FULL_TRIP_DOSSIER')),
    CONSTRAINT ck_audit_export_manifests_format CHECK (format IN ('JSON_LD', 'CSV', 'PRINTABLE_HTML'))
);

CREATE INDEX IF NOT EXISTS idx_audit_export_manifests_company ON audit_export_manifests (company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_export_manifests_trip ON audit_export_manifests (trip_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_export_manifests_digest ON audit_export_manifests (sha256_digest);

-- Seed initial demonstration manifest for Demo Logistics Corp
INSERT INTO audit_export_manifests (
    id, company_id, trip_id, export_type, format, sha256_digest, record_count, generated_by_user_id, metadata, created_at
) VALUES (
    '00000000-0000-4000-8600-000000000001',
    '00000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-9000-000000000001',
    'COLD_CHAIN_EXCURSION',
    'JSON_LD',
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    12,
    '00000000-0000-4000-a000-000000000001',
    '{"regulatoryStandard": "FDA 21 CFR Part 11 / GDP", "notes": "Baseline demo compliance certification"}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '2 hours'
) ON CONFLICT (id) DO NOTHING;
