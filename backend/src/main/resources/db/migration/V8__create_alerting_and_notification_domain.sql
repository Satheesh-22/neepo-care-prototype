-- V8: Multi-channel real-time alerting and webhooks engine domain

CREATE TABLE IF NOT EXISTS notification_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    channel_type VARCHAR(30) NOT NULL DEFAULT 'WEBHOOK',
    target_endpoint VARCHAR(500) NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    minimum_severity VARCHAR(30) NOT NULL DEFAULT 'LOW',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_notification_channels_type CHECK (channel_type IN ('WEBHOOK', 'SMS', 'WEBPUSH')),
    CONSTRAINT ck_notification_channels_severity CHECK (minimum_severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_notification_channels_company ON notification_channels (company_id);
CREATE INDEX IF NOT EXISTS idx_notification_channels_active ON notification_channels (company_id, active);

CREATE TABLE IF NOT EXISTS notification_delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES notification_channels (id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    disruption_event_id UUID,
    disruption_type VARCHAR(60) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    signature VARCHAR(100),
    payload TEXT,
    error_message VARCHAR(500),
    delivered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_notification_delivery_logs_status CHECK (status IN ('DELIVERED', 'SUPPRESSED_BY_SEVERITY', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_notification_delivery_company ON notification_delivery_logs (company_id);
CREATE INDEX IF NOT EXISTS idx_notification_delivery_channel ON notification_delivery_logs (channel_id);
CREATE INDEX IF NOT EXISTS idx_notification_delivery_delivered_at ON notification_delivery_logs (company_id, delivered_at DESC);

-- Seed default demo notification channels for Demo Logistics Corp
INSERT INTO notification_channels (id, company_id, name, channel_type, target_endpoint, secret_key, minimum_severity, active, created_at)
VALUES 
    ('00000000-0000-4000-8500-000000000001', '00000000-0000-4000-8000-000000000001', 'Operations Center Slack Webhook', 'WEBHOOK', 'https://hooks.slack.com/services/NEEPO/OPS/ALERT_CHANNEL', 'neepo_secret_ops_slack_2026', 'MEDIUM', TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-4000-8500-000000000002', '00000000-0000-4000-8000-000000000001', 'Fleet Emergency PagerDuty', 'WEBHOOK', 'https://events.pagerduty.com/v2/enqueue', 'neepo_secret_pd_emergency_2026', 'HIGH', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Seed initial delivery log for demonstration
INSERT INTO notification_delivery_logs (id, channel_id, company_id, disruption_event_id, disruption_type, severity, status, signature, payload, error_message, delivered_at)
VALUES 
    ('00000000-0000-4000-8500-000000000101', '00000000-0000-4000-8500-000000000001', '00000000-0000-4000-8000-000000000001', NULL, 'SYSTEM_STARTUP', 'LOW', 'DELIVERED', 'a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890', '{"event":"SYSTEM_STARTUP","status":"READY"}', NULL, CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT (id) DO NOTHING;
