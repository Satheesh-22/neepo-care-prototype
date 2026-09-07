-- V7: Onboarding and role-based administration domain enhancements

-- 1. Company profile attributes
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS legal_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS trading_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS registration_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS industry VARCHAR(80),
    ADD COLUMN IF NOT EXISTS address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state_province VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS contact_person VARCHAR(100),
    ADD COLUMN IF NOT EXISTS business_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(60) NOT NULL DEFAULT 'UTC',
    ADD COLUMN IF NOT EXISTS measurement_units VARCHAR(20) NOT NULL DEFAULT 'METRIC',
    ADD COLUMN IF NOT EXISTS cargo_temperature_policy VARCHAR(60) NOT NULL DEFAULT 'STRICT_COLD_CHAIN',
    ADD COLUMN IF NOT EXISTS logo_reference VARCHAR(255),
    ADD COLUMN IF NOT EXISTS data_retention_days INTEGER NOT NULL DEFAULT 90;

-- Backfill existing demo company data
UPDATE companies
SET legal_name = COALESCE(legal_name, name),
    trading_name = COALESCE(trading_name, name),
    registration_number = COALESCE(registration_number, 'REG-' || code),
    industry = COALESCE(industry, 'Cold Chain Logistics'),
    address = COALESCE(address, '100 Logistics Blvd, Suite 400'),
    city = COALESCE(city, 'San Francisco'),
    state_province = COALESCE(state_province, 'CA'),
    country = COALESCE(country, 'USA'),
    postal_code = COALESCE(postal_code, '94105'),
    contact_person = COALESCE(contact_person, 'Operations Lead'),
    business_email = COALESCE(business_email, 'ops@neepo.local'),
    phone_number = COALESCE(phone_number, '+15550100000'),
    timezone = COALESCE(timezone, 'UTC'),
    measurement_units = COALESCE(measurement_units, 'METRIC'),
    cargo_temperature_policy = COALESCE(cargo_temperature_policy, 'STRICT_COLD_CHAIN'),
    logo_reference = COALESCE(logo_reference, '/neepo-logo.jpg'),
    data_retention_days = COALESCE(data_retention_days, 90)
WHERE registration_number IS NULL OR legal_name IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_companies_registration_number ON companies (registration_number);

-- 2. User status & administration
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMPTZ;

-- 3. User invitations
CREATE TABLE IF NOT EXISTS user_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_by_user_id UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_user_invitations_role CHECK (role IN ('ADMIN', 'DISPATCHER', 'DRIVER'))
);

CREATE INDEX IF NOT EXISTS idx_user_invitations_company ON user_invitations (company_id);
CREATE INDEX IF NOT EXISTS idx_user_invitations_user ON user_invitations (user_id);
CREATE INDEX IF NOT EXISTS idx_user_invitations_expires ON user_invitations (expires_at);

-- 4. Driver profile extensions
ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS employee_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS license_category VARCHAR(30) NOT NULL DEFAULT 'HEAVY_RIGID',
    ADD COLUMN IF NOT EXISTS license_expiry_date DATE,
    ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS base_location VARCHAR(150),
    ADD COLUMN IF NOT EXISTS employment_status VARCHAR(30) NOT NULL DEFAULT 'FULL_TIME',
    ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verification_state VARCHAR(30) NOT NULL DEFAULT 'VERIFIED',
    ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMPTZ;

-- Backfill demo driver details
UPDATE drivers d
SET employee_id = COALESCE(d.employee_id, CASE WHEN d.id = '00000000-0000-4000-8200-000000000001'::uuid THEN 'EMP-DRV-001' ELSE 'EMP-' || SUBSTRING(d.id::text, 1, 8) END),
    first_name = COALESCE(d.first_name, CASE WHEN d.id = '00000000-0000-4000-8200-000000000001'::uuid THEN 'Demo' ELSE 'Fleet' END),
    last_name = COALESCE(d.last_name, CASE WHEN d.id = '00000000-0000-4000-8200-000000000001'::uuid THEN 'Driver' ELSE 'Operator' END),
    email = COALESCE(d.email, u.email, 'driver-' || SUBSTRING(d.id::text, 1, 8) || '@example.com'),
    phone = COALESCE(d.phone, u.phone, '+15550100099'),
    license_category = COALESCE(d.license_category, 'HEAVY_RIGID'),
    license_expiry_date = COALESCE(d.license_expiry_date, CURRENT_DATE + INTERVAL '365 days'),
    emergency_contact_name = COALESCE(d.emergency_contact_name, 'Support Operations'),
    emergency_contact_phone = COALESCE(d.emergency_contact_phone, '+15550100099'),
    base_location = COALESCE(d.base_location, 'Primary Terminal Bay 1'),
    employment_status = COALESCE(d.employment_status, 'FULL_TIME'),
    verification_state = COALESCE(d.verification_state, 'VERIFIED')
FROM users u
WHERE d.user_id = u.id AND (d.employee_id IS NULL OR d.first_name IS NULL);

-- Also catch any remaining drivers without matching user row
UPDATE drivers d
SET employee_id = COALESCE(d.employee_id, 'EMP-' || SUBSTRING(d.id::text, 1, 8)),
    first_name = COALESCE(d.first_name, 'Fleet'),
    last_name = COALESCE(d.last_name, 'Driver'),
    email = COALESCE(d.email, 'driver-' || SUBSTRING(d.id::text, 1, 8) || '@example.com'),
    phone = COALESCE(d.phone, '+15550100099'),
    license_category = COALESCE(d.license_category, 'HEAVY_RIGID'),
    license_expiry_date = COALESCE(d.license_expiry_date, CURRENT_DATE + INTERVAL '365 days'),
    emergency_contact_name = COALESCE(d.emergency_contact_name, 'Support Operations'),
    emergency_contact_phone = COALESCE(d.emergency_contact_phone, '+15550100099'),
    base_location = COALESCE(d.base_location, 'Primary Terminal Bay 1'),
    employment_status = COALESCE(d.employment_status, 'FULL_TIME'),
    verification_state = COALESCE(d.verification_state, 'VERIFIED')
WHERE d.employee_id IS NULL OR d.first_name IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_drivers_company_employee_id ON drivers (company_id, employee_id);

-- 5. Vehicle profile extensions
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS vin VARCHAR(50),
    ADD COLUMN IF NOT EXISTS make VARCHAR(80),
    ADD COLUMN IF NOT EXISTS model VARCHAR(80),
    ADD COLUMN IF NOT EXISTS year INTEGER,
    ADD COLUMN IF NOT EXISTS permitted_cargo_types TEXT[] NOT NULL DEFAULT ARRAY['GENERAL', 'REFRIGERATED', 'FROZEN', 'FRAGILE', 'HAZARDOUS']::TEXT[],
    ADD COLUMN IF NOT EXISTS cargo_temperature_min_c NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS cargo_temperature_max_c NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS device_identifier VARCHAR(100);

-- Backfill demo vehicle details
UPDATE vehicles
SET vin = COALESCE(vin, CASE WHEN id = '00000000-0000-4000-8300-000000000001'::uuid THEN 'VIN-NEEPO-001' ELSE 'VIN-' || UPPER(REPLACE(registration_number, '-', '')) END),
    make = COALESCE(make, 'Volvo'),
    model = COALESCE(model, 'FH Electric Reefer'),
    year = COALESCE(year, 2025),
    device_identifier = COALESCE(device_identifier, CASE WHEN id = '00000000-0000-4000-8300-000000000001'::uuid THEN 'DEV-OBD-001' ELSE 'DEV-' || UPPER(REPLACE(registration_number, '-', '')) END),
    cargo_temperature_min_c = COALESCE(cargo_temperature_min_c, -25.0),
    cargo_temperature_max_c = COALESCE(cargo_temperature_max_c, 10.0)
WHERE vin IS NULL OR make IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vehicles_company_vin ON vehicles (company_id, vin);

-- 6. Direct Driver-to-Vehicle assignments table
CREATE TABLE IF NOT EXISTS driver_vehicle_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    driver_id UUID NOT NULL REFERENCES drivers (id),
    vehicle_id UUID NOT NULL REFERENCES vehicles (id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    notes VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    assigned_by_user_id UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMPTZ,
    CONSTRAINT ck_dva_status CHECK (status IN ('ACTIVE', 'RELEASED', 'CANCELLED')),
    CONSTRAINT ck_dva_period CHECK (end_time IS NULL OR end_time >= start_time),
    CONSTRAINT ck_dva_release CHECK (released_at IS NULL OR released_at >= start_time)
);

CREATE INDEX IF NOT EXISTS idx_dva_company_driver ON driver_vehicle_assignments (company_id, driver_id, status);
CREATE INDEX IF NOT EXISTS idx_dva_company_vehicle ON driver_vehicle_assignments (company_id, vehicle_id, status);
CREATE INDEX IF NOT EXISTS idx_dva_created_at ON driver_vehicle_assignments (created_at DESC);
