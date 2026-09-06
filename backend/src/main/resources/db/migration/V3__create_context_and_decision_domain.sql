CREATE TABLE telemetry_observations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    trip_id UUID NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES vehicles (id),
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    engine_temperature_c NUMERIC(6, 2),
    cargo_temperature_c NUMERIC(6, 2),
    speed_kph NUMERIC(7, 2),
    confidence NUMERIC(4, 3) NOT NULL,
    observation_status VARCHAR(20) NOT NULL,
    source VARCHAR(30) NOT NULL,
    network_state VARCHAR(20) NOT NULL,
    raw_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_telemetry_event UNIQUE (event_id),
    CONSTRAINT ck_telemetry_times CHECK (
        received_at >= recorded_at AND expires_at > recorded_at
    ),
    CONSTRAINT ck_telemetry_confidence CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_telemetry_speed CHECK (speed_kph IS NULL OR speed_kph >= 0),
    CONSTRAINT ck_observation_status CHECK (
        observation_status IN ('FRESH', 'AGING', 'EXPIRED', 'UNVERIFIED')
    ),
    CONSTRAINT ck_telemetry_source CHECK (
        source IN ('SIMULATOR', 'DRIVER_DEVICE', 'VEHICLE_DEVICE')
    ),
    CONSTRAINT ck_network_state CHECK (
        network_state IN ('ONLINE', 'WEAK', 'OFFLINE', 'SYNCHRONIZING')
    )
);

CREATE TABLE trip_context_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    snapshot_sequence BIGINT NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    minimum_confidence NUMERIC(4, 3) NOT NULL,
    has_expired_required_data BOOLEAN NOT NULL,
    network_state VARCHAR(20) NOT NULL,
    context_payload JSONB NOT NULL,
    context_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_trip_context_sequence UNIQUE (trip_id, snapshot_sequence),
    CONSTRAINT ck_context_confidence CHECK (minimum_confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_context_network_state CHECK (
        network_state IN ('ONLINE', 'WEAK', 'OFFLINE', 'SYNCHRONIZING')
    )
);

CREATE TABLE disruption_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    trip_id UUID NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    context_snapshot_id UUID NOT NULL REFERENCES trip_context_snapshots (id),
    disruption_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    explanation_codes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    evidence JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_disruption_event UNIQUE (event_id),
    CONSTRAINT ck_disruption_type CHECK (
        disruption_type IN (
            'VEHICLE_OVERHEATING', 'VEHICLE_BREAKDOWN', 'ROUTE_BLOCKED',
            'ROUTE_DEVIATION', 'CARGO_TEMPERATURE_DEVIATION',
            'DRIVER_REST_REQUIRED', 'PROVIDER_CANCELLED', 'UNSAFE_STOP_LOCATION'
        )
    ),
    CONSTRAINT ck_disruption_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE recovery_plans (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    disruption_id UUID NOT NULL REFERENCES disruption_events (id) ON DELETE CASCADE,
    plan_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_id UUID REFERENCES service_providers (id),
    safe_stop_id UUID REFERENCES safe_stops (id),
    generated_at TIMESTAMPTZ NOT NULL,
    selected_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    generation_sequence INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_plan_generation_sequence UNIQUE (disruption_id, generation_sequence),
    CONSTRAINT ck_recovery_plan_type CHECK (
        plan_type IN (
            'WORKSHOP_REPAIR', 'SAFE_PARKING_MOBILE_MECHANIC',
            'TOW_REPAIR_OR_TRANSFER', 'SUBSTITUTE_VEHICLE_TRANSFER',
            'DRIVER_REST', 'EMERGENCY_ESCALATION'
        )
    ),
    CONSTRAINT ck_recovery_plan_status CHECK (
        status IN (
            'GENERATED', 'EVALUATING', 'FEASIBLE', 'REJECTED', 'SELECTED',
            'EXECUTING', 'INVALIDATED', 'COMPLETED', 'FAILED'
        )
    )
);

CREATE TABLE recovery_plan_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES recovery_plans (id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    action_code VARCHAR(80) NOT NULL,
    instruction VARCHAR(500) NOT NULL,
    provider_id UUID REFERENCES service_providers (id),
    expected_start_at TIMESTAMPTZ NOT NULL,
    expected_complete_at TIMESTAMPTZ NOT NULL,
    required_resources JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recovery_plan_step UNIQUE (plan_id, sequence_number),
    CONSTRAINT ck_plan_step_sequence CHECK (sequence_number > 0),
    CONSTRAINT ck_plan_step_times CHECK (expected_complete_at >= expected_start_at)
);

CREATE TABLE plan_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL UNIQUE REFERENCES recovery_plans (id) ON DELETE CASCADE,
    feasible BOOLEAN NOT NULL,
    violated_hard_constraints TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    warnings TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    soft_constraint_results JSONB NOT NULL DEFAULT '{}'::JSONB,
    risk_score NUMERIC(10, 4) NOT NULL,
    uncertainty_score NUMERIC(10, 4) NOT NULL,
    benefit_score NUMERIC(10, 4) NOT NULL,
    lambda_coefficient NUMERIC(10, 4) NOT NULL,
    mu_coefficient NUMERIC(10, 4) NOT NULL,
    final_utility NUMERIC(12, 4) NOT NULL,
    confidence NUMERIC(4, 3) NOT NULL,
    freshness_summary JSONB NOT NULL,
    explanation_codes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    evaluated_at TIMESTAMPTZ NOT NULL,
    evaluator_version VARCHAR(40) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_plan_evaluation_scores CHECK (
        risk_score BETWEEN 0 AND 100
        AND uncertainty_score BETWEEN 0 AND 100
        AND benefit_score BETWEEN 0 AND 100
        AND confidence BETWEEN 0 AND 1
        AND lambda_coefficient >= 0
        AND mu_coefficient >= 0
    ),
    CONSTRAINT ck_feasible_has_no_hard_violations CHECK (
        NOT feasible OR cardinality(violated_hard_constraints) = 0
    )
);

CREATE TABLE plan_invalidations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES recovery_plans (id),
    reason VARCHAR(50) NOT NULL,
    explanation_code VARCHAR(100) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    details VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plan_invalidation_event UNIQUE (event_id),
    CONSTRAINT ck_plan_invalidation_reason CHECK (
        reason IN (
            'PROVIDER_CANCELLED', 'PROVIDER_AVAILABILITY_EXPIRED',
            'ETA_THRESHOLD_EXCEEDED', 'VEHICLE_CONDITION_WORSENED',
            'ROUTE_BLOCKED', 'CARGO_SAFETY_THREATENED',
            'DRIVER_TIME_INSUFFICIENT', 'RESOURCE_UNAVAILABLE',
            'HIGHER_SEVERITY_DISRUPTION'
        )
    )
);

CREATE TABLE replanning_cycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    disruption_id UUID NOT NULL REFERENCES disruption_events (id),
    invalidation_id UUID REFERENCES plan_invalidations (id),
    cycle_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    selected_plan_id UUID REFERENCES recovery_plans (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_replanning_cycle UNIQUE (disruption_id, cycle_number),
    CONSTRAINT ck_replanning_cycle_number CHECK (cycle_number > 0),
    CONSTRAINT ck_replanning_status CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_replanning_times CHECK (completed_at IS NULL OR completed_at >= started_at)
);

CREATE TABLE telemetry_policy_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    event_id UUID NOT NULL,
    policy_state VARCHAR(40) NOT NULL,
    interval_seconds INTEGER,
    decided_at TIMESTAMPTZ NOT NULL,
    explanation_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_telemetry_policy_event UNIQUE (event_id),
    CONSTRAINT ck_telemetry_policy_state CHECK (
        policy_state IN (
            'NORMAL_TRAVEL', 'NEAR_DECISION_POINT', 'ROUTE_DEVIATION',
            'BREAKDOWN_OR_EMERGENCY', 'SAFELY_PARKED', 'OFFLINE_BUFFERING'
        )
    ),
    CONSTRAINT ck_telemetry_interval CHECK (
        (policy_state = 'OFFLINE_BUFFERING' AND interval_seconds IS NULL)
        OR (policy_state <> 'OFFLINE_BUFFERING' AND interval_seconds > 0)
    )
);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    result_reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id UUID NOT NULL,
    actor_user_id UUID REFERENCES users (id),
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE benchmark_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_code VARCHAR(80) NOT NULL,
    strategy_code VARCHAR(80) NOT NULL,
    dataset_version VARCHAR(40) NOT NULL,
    successful_recovery BOOLEAN NOT NULL,
    invalid_recommendations INTEGER NOT NULL,
    recovery_time_seconds INTEGER NOT NULL,
    rerouting_operations INTEGER NOT NULL,
    telemetry_transmissions INTEGER NOT NULL,
    estimated_bandwidth_bytes BIGINT NOT NULL,
    decision_latency_ms INTEGER NOT NULL,
    stale_data_decisions INTEGER NOT NULL,
    measured_at TIMESTAMPTZ NOT NULL,
    configuration JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_benchmark_nonnegative CHECK (
        invalid_recommendations >= 0
        AND recovery_time_seconds >= 0
        AND rerouting_operations >= 0
        AND telemetry_transmissions >= 0
        AND estimated_bandwidth_bytes >= 0
        AND decision_latency_ms >= 0
        AND stale_data_decisions >= 0
    )
);

CREATE INDEX idx_telemetry_trip_recorded ON telemetry_observations (trip_id, recorded_at DESC);
CREATE INDEX idx_telemetry_location_gist ON telemetry_observations USING GIST (location);
CREATE INDEX idx_context_trip_captured ON trip_context_snapshots (trip_id, captured_at DESC);
CREATE INDEX idx_disruption_trip_active ON disruption_events (trip_id, active, detected_at DESC);
CREATE INDEX idx_recovery_plan_disruption_status ON recovery_plans (disruption_id, status);
CREATE UNIQUE INDEX uk_selected_plan_per_disruption
    ON recovery_plans (disruption_id)
    WHERE status IN ('SELECTED', 'EXECUTING');
CREATE INDEX idx_invalidation_plan ON plan_invalidations (plan_id, observed_at DESC);
CREATE INDEX idx_replanning_trip ON replanning_cycles (trip_id, started_at DESC);
CREATE INDEX idx_audit_aggregate ON audit_events (aggregate_type, aggregate_id, occurred_at DESC);
