-- Flyway Migration V11: Hardware Telematics Gateway & Fleet Pilot Domain
-- Ephemeral & Persistent Schema for Physical Telematics Gateways, Flash Replay Logs, and OTA Policy

CREATE TABLE IF NOT EXISTS hardware_gateways (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    vehicle_id UUID REFERENCES vehicles(id),
    device_serial VARCHAR(64) NOT NULL,
    imei VARCHAR(32),
    secure_element_id VARCHAR(64),
    firmware_version VARCHAR(32) NOT NULL DEFAULT 'v1.1.0-prod',
    hmac_secret_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat_at TIMESTAMPTZ,
    last_sync_event_id UUID,
    buffer_capacity_records INTEGER NOT NULL DEFAULT 10000,
    reporting_interval_seconds INTEGER NOT NULL DEFAULT 5,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_gateways_company_serial UNIQUE (company_id, device_serial),
    CONSTRAINT ck_gateways_status CHECK (status IN ('REGISTERED', 'ACTIVE', 'OFFLINE', 'SUSPENDED'))
);

CREATE INDEX IF NOT EXISTS idx_gateways_company_status ON hardware_gateways (company_id, status);
CREATE INDEX IF NOT EXISTS idx_gateways_vehicle ON hardware_gateways (vehicle_id);

CREATE TABLE IF NOT EXISTS gateway_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    gateway_id UUID NOT NULL REFERENCES hardware_gateways(id) ON DELETE CASCADE,
    trip_id UUID REFERENCES trips(id),
    batch_size INTEGER NOT NULL,
    deduplicated_count INTEGER NOT NULL DEFAULT 0,
    sync_mode VARCHAR(32) NOT NULL DEFAULT 'STREAM',
    hmac_verified BOOLEAN NOT NULL DEFAULT TRUE,
    network_carrier VARCHAR(64),
    signal_rssi INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_gateway_sync_mode CHECK (sync_mode IN ('STREAM', 'OFFLINE_REPLAY', 'CELLULAR_HANDOFF'))
);

CREATE INDEX IF NOT EXISTS idx_gateway_sync_logs_company ON gateway_sync_logs (company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_gateway_sync_logs_gateway ON gateway_sync_logs (gateway_id, created_at DESC);

-- Seed repeatable baseline hardware gateway for demo vehicle
INSERT INTO hardware_gateways (
    id, company_id, vehicle_id, device_serial, imei, secure_element_id,
    firmware_version, hmac_secret_hash, status, last_heartbeat_at,
    buffer_capacity_records, reporting_interval_seconds, metadata
) VALUES (
    '00000000-0000-4000-8900-000000000001',
    '00000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8300-000000000001',
    'NEEPO-GW-2026-001',
    '867530901234567',
    'ATECC608A-77A9-B12C',
    'v1.1.0-prod',
    'bench_hardware_device_secret_secure_key_123',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    10000,
    5,
    '{"mcu": "ESP32-S3", "cellularModem": "Quectel EG915N-EU", "canBusProtocol": "J1939", "flashStorage": "16MB SPI NOR"}'::jsonb
) ON CONFLICT DO NOTHING;
