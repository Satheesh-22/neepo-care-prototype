-- V10: Fleet reliability, MTTR, and recovery cost-avoidance analytics domain

CREATE TABLE IF NOT EXISTS fleet_analytics_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    timeframe VARCHAR(10) NOT NULL,
    total_trips_monitored INT NOT NULL DEFAULT 0,
    active_disruptions_count INT NOT NULL DEFAULT 0,
    resolved_disruptions_count INT NOT NULL DEFAULT 0,
    mean_time_to_recovery_minutes NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    driver_compliance_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    estimated_cost_avoidance_usd NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    saved_transit_hours NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    spoilage_avoidance_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    root_causes_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    trends_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    strategies_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    snapshot_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fleet_analytics_timeframe CHECK (timeframe IN ('7d', '30d', '90d'))
);

CREATE INDEX IF NOT EXISTS idx_fleet_analytics_company ON fleet_analytics_snapshots (company_id, timeframe);
CREATE INDEX IF NOT EXISTS idx_fleet_analytics_snapshot_at ON fleet_analytics_snapshots (company_id, snapshot_at DESC);

-- Seed repeatable baseline analytics snapshots for Demo Logistics Corp (00000000-0000-4000-8000-000000000001)

-- 7-Day Window Snapshot
INSERT INTO fleet_analytics_snapshots (
    id, company_id, timeframe,
    total_trips_monitored, active_disruptions_count, resolved_disruptions_count,
    mean_time_to_recovery_minutes, driver_compliance_rate, estimated_cost_avoidance_usd,
    saved_transit_hours, spoilage_avoidance_rate,
    root_causes_json, trends_json, strategies_json, snapshot_at
) VALUES (
    '00000000-0000-4000-8700-000000000001',
    '00000000-0000-4000-8000-000000000001',
    '7d',
    14, 1, 8,
    18.50, 98.20, 34200.00,
    42.50, 100.00,
    '[
        {"cause": "Coolant & Thermal Limit Exceeded", "count": 4, "percentage": 44.4, "averageResolutionMinutes": 16.0},
        {"cause": "Corridor Blockage & Congestion", "count": 2, "percentage": 22.2, "averageResolutionMinutes": 24.5},
        {"cause": "Refrigeration Cold-Chain Drift", "count": 2, "percentage": 22.2, "averageResolutionMinutes": 14.0},
        {"cause": "Mandatory Rest Window Required", "count": 1, "percentage": 11.1, "averageResolutionMinutes": 45.0}
    ]'::jsonb,
    '[
        {"period": "Day 1", "disruptionCount": 1, "resolvedCount": 1, "costAvoidedUsd": 4200.00},
        {"period": "Day 2", "disruptionCount": 2, "resolvedCount": 2, "costAvoidedUsd": 7800.00},
        {"period": "Day 3", "disruptionCount": 0, "resolvedCount": 0, "costAvoidedUsd": 0.00},
        {"period": "Day 4", "disruptionCount": 1, "resolvedCount": 1, "costAvoidedUsd": 5100.00},
        {"period": "Day 5", "disruptionCount": 2, "resolvedCount": 2, "costAvoidedUsd": 8600.00},
        {"period": "Day 6", "disruptionCount": 1, "resolvedCount": 1, "costAvoidedUsd": 3900.00},
        {"period": "Day 7", "disruptionCount": 2, "resolvedCount": 1, "costAvoidedUsd": 4600.00}
    ]'::jsonb,
    '[
        {"strategyName": "Safe Parking & Mobile Mechanic", "usageCount": 4, "successRate": 100.0, "averageCostAvoidanceUsd": 8200.00},
        {"strategyName": "Authorized Workshop Repair", "usageCount": 3, "successRate": 100.0, "averageCostAvoidanceUsd": 9500.00},
        {"strategyName": "Substitute Vehicle Cargo Transfer", "usageCount": 1, "successRate": 100.0, "averageCostAvoidanceUsd": 16500.00}
    ]'::jsonb,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 30-Day Window Snapshot
INSERT INTO fleet_analytics_snapshots (
    id, company_id, timeframe,
    total_trips_monitored, active_disruptions_count, resolved_disruptions_count,
    mean_time_to_recovery_minutes, driver_compliance_rate, estimated_cost_avoidance_usd,
    saved_transit_hours, spoilage_avoidance_rate,
    root_causes_json, trends_json, strategies_json, snapshot_at
) VALUES (
    '00000000-0000-4000-8700-000000000002',
    '00000000-0000-4000-8000-000000000001',
    '30d',
    68, 1, 37,
    21.20, 96.80, 148500.00,
    186.00, 99.40,
    '[
        {"cause": "Coolant & Thermal Limit Exceeded", "count": 18, "percentage": 47.4, "averageResolutionMinutes": 17.5},
        {"cause": "Corridor Blockage & Congestion", "count": 9, "percentage": 23.7, "averageResolutionMinutes": 26.0},
        {"cause": "Refrigeration Cold-Chain Drift", "count": 7, "percentage": 18.4, "averageResolutionMinutes": 15.2},
        {"cause": "Mandatory Rest Window Required", "count": 4, "percentage": 10.5, "averageResolutionMinutes": 48.0}
    ]'::jsonb,
    '[
        {"period": "Week 1", "disruptionCount": 9, "resolvedCount": 9, "costAvoidedUsd": 38400.00},
        {"period": "Week 2", "disruptionCount": 11, "resolvedCount": 11, "costAvoidedUsd": 44200.00},
        {"period": "Week 3", "disruptionCount": 8, "resolvedCount": 8, "costAvoidedUsd": 31500.00},
        {"period": "Week 4", "disruptionCount": 10, "resolvedCount": 9, "costAvoidedUsd": 34400.00}
    ]'::jsonb,
    '[
        {"strategyName": "Safe Parking & Mobile Mechanic", "usageCount": 19, "successRate": 94.7, "averageCostAvoidanceUsd": 7800.00},
        {"strategyName": "Authorized Workshop Repair", "usageCount": 12, "successRate": 100.0, "averageCostAvoidanceUsd": 8900.00},
        {"strategyName": "Substitute Vehicle Cargo Transfer", "usageCount": 5, "successRate": 100.0, "averageCostAvoidanceUsd": 14200.00},
        {"strategyName": "Rest Window Re-assignment", "usageCount": 2, "successRate": 100.0, "averageCostAvoidanceUsd": 3100.00}
    ]'::jsonb,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 90-Day Window Snapshot
INSERT INTO fleet_analytics_snapshots (
    id, company_id, timeframe,
    total_trips_monitored, active_disruptions_count, resolved_disruptions_count,
    mean_time_to_recovery_minutes, driver_compliance_rate, estimated_cost_avoidance_usd,
    saved_transit_hours, spoilage_avoidance_rate,
    root_causes_json, trends_json, strategies_json, snapshot_at
) VALUES (
    '00000000-0000-4000-8700-000000000003',
    '00000000-0000-4000-8000-000000000001',
    '90d',
    196, 1, 112,
    23.40, 95.40, 435000.00,
    564.00, 98.90,
    '[
        {"cause": "Coolant & Thermal Limit Exceeded", "count": 52, "percentage": 46.0, "averageResolutionMinutes": 18.2},
        {"cause": "Corridor Blockage & Congestion", "count": 28, "percentage": 24.8, "averageResolutionMinutes": 27.4},
        {"cause": "Refrigeration Cold-Chain Drift", "count": 21, "percentage": 18.6, "averageResolutionMinutes": 16.0},
        {"cause": "Mandatory Rest Window Required", "count": 12, "percentage": 10.6, "averageResolutionMinutes": 51.0}
    ]'::jsonb,
    '[
        {"period": "Month 1", "disruptionCount": 38, "resolvedCount": 38, "costAvoidedUsd": 142000.00},
        {"period": "Month 2", "disruptionCount": 37, "resolvedCount": 37, "costAvoidedUsd": 144500.00},
        {"period": "Month 3", "disruptionCount": 38, "resolvedCount": 37, "costAvoidedUsd": 148500.00}
    ]'::jsonb,
    '[
        {"strategyName": "Safe Parking & Mobile Mechanic", "usageCount": 58, "successRate": 96.5, "averageCostAvoidanceUsd": 7650.00},
        {"strategyName": "Authorized Workshop Repair", "usageCount": 36, "successRate": 100.0, "averageCostAvoidanceUsd": 8800.00},
        {"strategyName": "Substitute Vehicle Cargo Transfer", "usageCount": 14, "successRate": 92.8, "averageCostAvoidanceUsd": 14600.00},
        {"strategyName": "Rest Window Re-assignment", "usageCount": 5, "successRate": 100.0, "averageCostAvoidanceUsd": 3200.00}
    ]'::jsonb,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
