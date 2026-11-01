-- Flyway Migration V13: Hierarchical Multi-Org Fleet Partitioning & Automated SCIM 2.0 User Provisioning
-- Schema for Depots, Fleet Hub Partitioning, SCIM External Identities, and SCIM Bearer Tokens

CREATE TABLE IF NOT EXISTS depots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(256) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50),
    postal_code VARCHAR(20),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    capacity INT NOT NULL DEFAULT 50,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_depot_code UNIQUE (company_id, code),
    CONSTRAINT ck_depot_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE')),
    CONSTRAINT ck_depot_capacity CHECK (capacity > 0)
);

CREATE INDEX IF NOT EXISTS idx_depots_company ON depots (company_id);
CREATE INDEX IF NOT EXISTS idx_depots_status ON depots (status);

-- Add depot reference to vehicles
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS depot_id UUID REFERENCES depots(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_vehicles_depot ON vehicles (depot_id);

-- Add depot reference and SCIM external ID to users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS depot_id UUID REFERENCES depots(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS scim_external_id VARCHAR(256);
CREATE INDEX IF NOT EXISTS idx_users_depot ON users (depot_id);
CREATE INDEX IF NOT EXISTS idx_users_scim_external_id ON users (scim_external_id);

-- Add SCIM configuration fields to company_sso_configs
ALTER TABLE company_sso_configs
    ADD COLUMN IF NOT EXISTS scim_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS scim_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS scim_token_prefix VARCHAR(32);

-- Seed baseline depots for Demo Company
INSERT INTO depots (
    id, company_id, code, name, address, city, state, postal_code,
    latitude, longitude, capacity, status, contact_phone, contact_email
) VALUES
(
    '00000000-0000-4000-8400-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'DEPOT-HQ-01',
    'Seattle Central Cold Chain Depot',
    '1000 Logistics Way, Industrial District',
    'Seattle',
    'WA',
    '98134',
    47.5800,
    -122.3300,
    45,
    'ACTIVE',
    '+12065550191',
    'seattle-depot@neepo.com'
),
(
    '00000000-0000-4000-8400-000000000002',
    '00000000-0000-4000-8000-000000000001',
    'DEPOT-PORT-02',
    'Portland Distribution Hub',
    '4500 Riverfront Expressway',
    'Portland',
    'OR',
    '97210',
    45.5400,
    -122.6900,
    30,
    'ACTIVE',
    '+15035550182',
    'portland-hub@neepo.com'
)
ON CONFLICT (id) DO NOTHING;

-- Associate demo vehicles and users with baseline depot
UPDATE vehicles
SET depot_id = '00000000-0000-4000-8400-000000000001'
WHERE id = '00000000-0000-4000-8300-000000000001' AND depot_id IS NULL;

UPDATE users
SET depot_id = '00000000-0000-4000-8400-000000000001'
WHERE id = '00000000-0000-4000-8100-000000000001' AND depot_id IS NULL;
