INSERT INTO companies (id, name, code)
VALUES ('00000000-0000-4000-8000-000000000001', 'NEEPO Demo Logistics', 'NEEPO-DEMO');

INSERT INTO users (id, company_id, name, email, phone, role)
VALUES
    (
        '00000000-0000-4000-8100-000000000001',
        '00000000-0000-4000-8000-000000000001',
        'Demo Dispatcher',
        'dispatcher@neepo.local',
        '+919999999991',
        'DISPATCHER'
    ),
    (
        '00000000-0000-4000-8100-000000000002',
        '00000000-0000-4000-8000-000000000001',
        'Demo Driver',
        'driver@neepo.local',
        '+919999999992',
        'DRIVER'
    );

INSERT INTO drivers (
    id, company_id, user_id, license_number, status,
    max_daily_driving_minutes, remaining_driving_minutes, last_rest_completed_at
)
VALUES (
    '00000000-0000-4000-8200-000000000001',
    '00000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8100-000000000002',
    'TN-DEMO-2026-001',
    'ASSIGNED',
    540,
    360,
    CURRENT_TIMESTAMP - INTERVAL '3 hours'
);

INSERT INTO vehicles (
    id, company_id, registration_number, vehicle_type, status,
    height_meters, width_meters, gross_weight_kg, payload_capacity_kg,
    maximum_engine_temperature_c, refrigeration_supported, current_engine_health_percent
)
VALUES (
    '00000000-0000-4000-8300-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'TN-NEEPO-01',
    'REFRIGERATED_TRUCK',
    'ASSIGNED',
    3.80,
    2.50,
    14000,
    7000,
    105,
    TRUE,
    92
);

INSERT INTO vehicle_constraints (
    id, vehicle_id, constraint_code, constraint_kind, numeric_value, unit, details
)
VALUES
    (
        '00000000-0000-4000-8310-000000000001',
        '00000000-0000-4000-8300-000000000001',
        'MAX_ENGINE_TEMPERATURE',
        'HARD',
        105,
        'CELSIUS',
        'Overheating disruption begins above this temperature.'
    ),
    (
        '00000000-0000-4000-8310-000000000002',
        '00000000-0000-4000-8300-000000000001',
        'REQUIRES_HEAVY_VEHICLE_SERVICE',
        'HARD',
        NULL,
        NULL,
        'Provider must support refrigerated heavy vehicles.'
    );

INSERT INTO cargo (
    id, company_id, reference_code, cargo_type, description, weight_kg,
    minimum_temperature_c, maximum_temperature_c, hazardous
)
VALUES (
    '00000000-0000-4000-8400-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'CARGO-DEMO-001',
    'REFRIGERATED',
    'Temperature-sensitive diagnostic reagents',
    2500,
    2,
    8,
    FALSE
);

INSERT INTO cargo_constraints (
    id, cargo_id, constraint_code, constraint_kind, numeric_value, unit, details
)
VALUES
    (
        '00000000-0000-4000-8410-000000000001',
        '00000000-0000-4000-8400-000000000001',
        'MIN_TEMPERATURE',
        'HARD',
        2,
        'CELSIUS',
        'Cargo must not fall below 2°C.'
    ),
    (
        '00000000-0000-4000-8410-000000000002',
        '00000000-0000-4000-8400-000000000001',
        'MAX_TEMPERATURE',
        'HARD',
        8,
        'CELSIUS',
        'Cargo must not exceed 8°C.'
    );

INSERT INTO routes (
    id, company_id, name, origin, destination, planned_path,
    planned_distance_km, planned_duration_minutes, status
)
VALUES (
    '00000000-0000-4000-8500-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'Erode to Bengaluru Demo Route',
    ST_SetSRID(ST_MakePoint(77.7172, 11.3410), 4326)::geography,
    ST_SetSRID(ST_MakePoint(77.5946, 12.9716), 4326)::geography,
    ST_SetSRID(
        ST_MakeLine(
            ST_MakePoint(77.7172, 11.3410),
            ST_MakePoint(77.5946, 12.9716)
        ),
        4326
    )::geography,
    260,
    360,
    'PLANNED'
);

INSERT INTO route_restrictions (
    id, route_id, restriction_code, restriction_kind, limit_value, unit,
    observed_at, expires_at, confidence, details
)
VALUES (
    '00000000-0000-4000-8510-000000000001',
    '00000000-0000-4000-8500-000000000001',
    'WORKSHOP_A_ACCESS_HEIGHT',
    'HEIGHT',
    3.50,
    'METERS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    0.99,
    'Access road to the nearest workshop is limited to 3.5 m.'
);

INSERT INTO trips (
    id, company_id, vehicle_id, driver_id, cargo_id, route_id,
    reference_code, status, delivery_deadline,
    maximum_waiting_minutes, accepted_eta_threshold_minutes
)
VALUES (
    '00000000-0000-4000-8600-000000000001',
    '00000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8300-000000000001',
    '00000000-0000-4000-8200-000000000001',
    '00000000-0000-4000-8400-000000000001',
    '00000000-0000-4000-8500-000000000001',
    'TRIP-DEMO-001',
    'ASSIGNED',
    CURRENT_TIMESTAMP + INTERVAL '20 hours',
    180,
    90
);

INSERT INTO trip_assignments (
    id, trip_id, vehicle_id, driver_id, assigned_at
)
VALUES (
    '00000000-0000-4000-8610-000000000001',
    '00000000-0000-4000-8600-000000000001',
    '00000000-0000-4000-8300-000000000001',
    '00000000-0000-4000-8200-000000000001',
    CURRENT_TIMESTAMP
);

INSERT INTO service_providers (
    id, name, provider_type, location, service_radius_km, reliability_score, active
)
VALUES
    (
        '00000000-0000-4000-8700-000000000001',
        'Workshop A - Nearest Incompatible',
        'WORKSHOP',
        ST_SetSRID(ST_MakePoint(77.7000, 11.3800), 4326)::geography,
        100,
        88,
        TRUE
    ),
    (
        '00000000-0000-4000-8700-000000000002',
        'Workshop B - Compatible',
        'WORKSHOP',
        ST_SetSRID(ST_MakePoint(77.6500, 11.5000), 4326)::geography,
        150,
        94,
        TRUE
    ),
    (
        '00000000-0000-4000-8700-000000000003',
        'Workshop C - Stale Availability',
        'WORKSHOP',
        ST_SetSRID(ST_MakePoint(77.6200, 11.5500), 4326)::geography,
        150,
        92,
        TRUE
    ),
    (
        '00000000-0000-4000-8700-000000000004',
        'Mobile Mechanic D - Replanning Fallback',
        'MOBILE_MECHANIC',
        ST_SetSRID(ST_MakePoint(77.6800, 11.4500), 4326)::geography,
        180,
        91,
        TRUE
    ),
    (
        '00000000-0000-4000-8700-000000000005',
        'Towing Service E',
        'TOWING_SERVICE',
        ST_SetSRID(ST_MakePoint(77.6700, 11.4700), 4326)::geography,
        200,
        89,
        TRUE
    );

INSERT INTO provider_capabilities (id, provider_id, capability_code, details)
VALUES
    (
        '00000000-0000-4000-8710-000000000001',
        '00000000-0000-4000-8700-000000000001',
        'HEAVY_VEHICLE_REPAIR',
        'Workshop can repair heavy vehicles, but the access road is incompatible.'
    ),
    (
        '00000000-0000-4000-8710-000000000002',
        '00000000-0000-4000-8700-000000000002',
        'REFRIGERATED_HEAVY_VEHICLE_REPAIR',
        'Compatible repair provider selected during the initial plan.'
    ),
    (
        '00000000-0000-4000-8710-000000000003',
        '00000000-0000-4000-8700-000000000003',
        'REFRIGERATED_HEAVY_VEHICLE_REPAIR',
        'Capability is valid, but availability is expired.'
    ),
    (
        '00000000-0000-4000-8710-000000000004',
        '00000000-0000-4000-8700-000000000004',
        'MOBILE_ENGINE_COOLING_REPAIR',
        'Mobile fallback for automatic re-planning.'
    ),
    (
        '00000000-0000-4000-8710-000000000005',
        '00000000-0000-4000-8700-000000000005',
        'HEAVY_VEHICLE_TOWING',
        'Supports towing the demonstration truck.'
    );

INSERT INTO provider_availability (
    id, provider_id, status, observed_at, expires_at,
    estimated_arrival_minutes, confidence, required_resource_available
)
VALUES
    (
        '00000000-0000-4000-8720-000000000001',
        '00000000-0000-4000-8700-000000000001',
        'AVAILABLE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '10 minutes',
        20,
        0.98,
        TRUE
    ),
    (
        '00000000-0000-4000-8720-000000000002',
        '00000000-0000-4000-8700-000000000002',
        'AVAILABLE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '10 minutes',
        45,
        0.97,
        TRUE
    ),
    (
        '00000000-0000-4000-8720-000000000003',
        '00000000-0000-4000-8700-000000000003',
        'AVAILABLE',
        CURRENT_TIMESTAMP - INTERVAL '30 minutes',
        CURRENT_TIMESTAMP - INTERVAL '20 minutes',
        35,
        0.90,
        TRUE
    ),
    (
        '00000000-0000-4000-8720-000000000004',
        '00000000-0000-4000-8700-000000000004',
        'AVAILABLE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '10 minutes',
        55,
        0.96,
        TRUE
    ),
    (
        '00000000-0000-4000-8720-000000000005',
        '00000000-0000-4000-8700-000000000005',
        'AVAILABLE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '10 minutes',
        60,
        0.95,
        TRUE
    );

INSERT INTO safe_stops (
    id, name, location, maximum_vehicle_height_meters,
    maximum_vehicle_weight_kg, security_verified,
    observed_at, expires_at, confidence
)
VALUES
    (
        '00000000-0000-4000-8800-000000000001',
        'Safe Parking North',
        ST_SetSRID(ST_MakePoint(77.6900, 11.4300), 4326)::geography,
        5.0,
        25000,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '24 hours',
        0.98
    ),
    (
        '00000000-0000-4000-8800-000000000002',
        'Safe Parking South',
        ST_SetSRID(ST_MakePoint(77.7100, 11.4000), 4326)::geography,
        4.5,
        20000,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '24 hours',
        0.96
    );
