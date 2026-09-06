CREATE TABLE drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    user_id UUID NOT NULL UNIQUE REFERENCES users (id),
    license_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    max_daily_driving_minutes INTEGER NOT NULL,
    remaining_driving_minutes INTEGER NOT NULL,
    last_rest_completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_drivers_license UNIQUE (company_id, license_number),
    CONSTRAINT ck_drivers_status CHECK (
        status IN ('AVAILABLE', 'ASSIGNED', 'DRIVING', 'REST_REQUIRED', 'RESTING', 'OFF_DUTY')
    ),
    CONSTRAINT ck_driving_minutes CHECK (
        max_daily_driving_minutes > 0
        AND remaining_driving_minutes BETWEEN 0 AND max_daily_driving_minutes
    )
);

CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    registration_number VARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL,
    height_meters NUMERIC(6, 2) NOT NULL,
    width_meters NUMERIC(6, 2) NOT NULL,
    gross_weight_kg NUMERIC(12, 2) NOT NULL,
    payload_capacity_kg NUMERIC(12, 2) NOT NULL,
    maximum_engine_temperature_c NUMERIC(6, 2) NOT NULL,
    refrigeration_supported BOOLEAN NOT NULL DEFAULT FALSE,
    current_engine_health_percent NUMERIC(5, 2) NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_vehicles_registration UNIQUE (company_id, registration_number),
    CONSTRAINT ck_vehicle_status CHECK (
        status IN ('AVAILABLE', 'ASSIGNED', 'IN_TRANSIT', 'DISRUPTED', 'RECOVERY', 'MAINTENANCE', 'OUT_OF_SERVICE')
    ),
    CONSTRAINT ck_vehicle_dimensions CHECK (
        height_meters > 0 AND width_meters > 0 AND gross_weight_kg > 0 AND payload_capacity_kg > 0
    ),
    CONSTRAINT ck_vehicle_health CHECK (current_engine_health_percent BETWEEN 0 AND 100)
);

CREATE TABLE vehicle_constraints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    constraint_code VARCHAR(80) NOT NULL,
    constraint_kind VARCHAR(10) NOT NULL,
    numeric_value NUMERIC(14, 4),
    unit VARCHAR(30),
    details VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vehicle_constraint UNIQUE (vehicle_id, constraint_code),
    CONSTRAINT ck_vehicle_constraint_kind CHECK (constraint_kind IN ('HARD', 'SOFT'))
);

CREATE TABLE cargo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    reference_code VARCHAR(50) NOT NULL,
    cargo_type VARCHAR(30) NOT NULL,
    description VARCHAR(300) NOT NULL,
    weight_kg NUMERIC(12, 2) NOT NULL,
    minimum_temperature_c NUMERIC(6, 2),
    maximum_temperature_c NUMERIC(6, 2),
    hazardous BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_cargo_reference UNIQUE (company_id, reference_code),
    CONSTRAINT ck_cargo_type CHECK (
        cargo_type IN ('GENERAL', 'REFRIGERATED', 'FROZEN', 'HAZARDOUS', 'FRAGILE')
    ),
    CONSTRAINT ck_cargo_weight CHECK (weight_kg > 0),
    CONSTRAINT ck_cargo_temperature_range CHECK (
        minimum_temperature_c IS NULL
        OR maximum_temperature_c IS NULL
        OR minimum_temperature_c <= maximum_temperature_c
    )
);

CREATE TABLE cargo_constraints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cargo_id UUID NOT NULL REFERENCES cargo (id) ON DELETE CASCADE,
    constraint_code VARCHAR(80) NOT NULL,
    constraint_kind VARCHAR(10) NOT NULL,
    numeric_value NUMERIC(14, 4),
    unit VARCHAR(30),
    details VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cargo_constraint UNIQUE (cargo_id, constraint_code),
    CONSTRAINT ck_cargo_constraint_kind CHECK (constraint_kind IN ('HARD', 'SOFT'))
);

CREATE TABLE routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    name VARCHAR(150) NOT NULL,
    origin GEOGRAPHY(POINT, 4326) NOT NULL,
    destination GEOGRAPHY(POINT, 4326) NOT NULL,
    planned_path GEOGRAPHY(LINESTRING, 4326),
    planned_distance_km NUMERIC(10, 2) NOT NULL,
    planned_duration_minutes INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_route_status CHECK (status IN ('PLANNED', 'ACTIVE', 'BLOCKED', 'COMPLETED')),
    CONSTRAINT ck_route_plan_values CHECK (planned_distance_km > 0 AND planned_duration_minutes > 0)
);

CREATE TABLE route_restrictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    restriction_code VARCHAR(80) NOT NULL,
    restriction_kind VARCHAR(30) NOT NULL,
    limit_value NUMERIC(14, 4),
    unit VARCHAR(30),
    segment GEOGRAPHY(LINESTRING, 4326),
    observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    confidence NUMERIC(4, 3) NOT NULL,
    details VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_route_restriction UNIQUE (route_id, restriction_code),
    CONSTRAINT ck_route_restriction_kind CHECK (
        restriction_kind IN ('HEIGHT', 'WIDTH', 'WEIGHT', 'BLOCKAGE', 'HAZARD')
    ),
    CONSTRAINT ck_route_restriction_confidence CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_route_restriction_expiry CHECK (expires_at > observed_at)
);

CREATE TABLE trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    vehicle_id UUID NOT NULL REFERENCES vehicles (id),
    driver_id UUID NOT NULL REFERENCES drivers (id),
    cargo_id UUID NOT NULL REFERENCES cargo (id),
    route_id UUID NOT NULL REFERENCES routes (id),
    reference_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    delivery_deadline TIMESTAMPTZ NOT NULL,
    maximum_waiting_minutes INTEGER NOT NULL,
    accepted_eta_threshold_minutes INTEGER NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_trip_reference UNIQUE (company_id, reference_code),
    CONSTRAINT ck_trip_status CHECK (
        status IN ('CREATED', 'ASSIGNED', 'IN_PROGRESS', 'DISRUPTED', 'RECOVERY_IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT ck_trip_wait_values CHECK (
        maximum_waiting_minutes > 0 AND accepted_eta_threshold_minutes > 0
    )
);

CREATE TABLE trip_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES vehicles (id),
    driver_id UUID NOT NULL REFERENCES drivers (id),
    assigned_at TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_assignment_period CHECK (released_at IS NULL OR released_at >= assigned_at)
);

CREATE TABLE service_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID REFERENCES companies (id),
    name VARCHAR(150) NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    service_radius_km NUMERIC(10, 2) NOT NULL,
    reliability_score NUMERIC(5, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_provider_type CHECK (
        provider_type IN ('WORKSHOP', 'MOBILE_MECHANIC', 'TOWING_SERVICE', 'SAFE_PARKING', 'SUBSTITUTE_VEHICLE')
    ),
    CONSTRAINT ck_provider_scores CHECK (
        service_radius_km > 0 AND reliability_score BETWEEN 0 AND 100
    )
);

CREATE TABLE provider_capabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    capability_code VARCHAR(80) NOT NULL,
    details VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_capability UNIQUE (provider_id, capability_code)
);

CREATE TABLE provider_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    estimated_arrival_minutes INTEGER,
    confidence NUMERIC(4, 3) NOT NULL,
    required_resource_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_provider_availability_status CHECK (
        status IN ('AVAILABLE', 'BUSY', 'UNAVAILABLE', 'CANCELLED')
    ),
    CONSTRAINT ck_provider_availability_expiry CHECK (expires_at > observed_at),
    CONSTRAINT ck_provider_availability_confidence CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_provider_eta CHECK (
        estimated_arrival_minutes IS NULL OR estimated_arrival_minutes >= 0
    )
);

CREATE TABLE safe_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    maximum_vehicle_height_meters NUMERIC(6, 2),
    maximum_vehicle_weight_kg NUMERIC(12, 2),
    security_verified BOOLEAN NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    confidence NUMERIC(4, 3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_safe_stop_expiry CHECK (expires_at > observed_at),
    CONSTRAINT ck_safe_stop_confidence CHECK (confidence BETWEEN 0 AND 1)
);

CREATE INDEX idx_drivers_company_status ON drivers (company_id, status);
CREATE INDEX idx_vehicles_company_status ON vehicles (company_id, status);
CREATE INDEX idx_trips_company_status ON trips (company_id, status);
CREATE INDEX idx_trips_deadline ON trips (delivery_deadline);
CREATE INDEX idx_trip_assignments_trip ON trip_assignments (trip_id, assigned_at DESC);
CREATE INDEX idx_route_restrictions_route ON route_restrictions (route_id, expires_at);
CREATE INDEX idx_provider_availability_provider ON provider_availability (provider_id, observed_at DESC);
CREATE INDEX idx_routes_origin_gist ON routes USING GIST (origin);
CREATE INDEX idx_routes_destination_gist ON routes USING GIST (destination);
CREATE INDEX idx_routes_path_gist ON routes USING GIST (planned_path);
CREATE INDEX idx_providers_location_gist ON service_providers USING GIST (location);
CREATE INDEX idx_safe_stops_location_gist ON safe_stops USING GIST (location);
