-- Flyway Migration V12: Multi-Tenant Enterprise Administration & SSO Domain
-- Schema for OIDC / SAML 2.0 Identity Provider Federation, JIT User Provisioning, and SSO Audit Logs

CREATE TABLE IF NOT EXISTS company_sso_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    idp_type VARCHAR(32) NOT NULL DEFAULT 'OIDC',
    idp_issuer_url VARCHAR(255) NOT NULL,
    client_id VARCHAR(128) NOT NULL,
    client_secret_hash VARCHAR(128) NOT NULL,
    sso_domain VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    enforce_sso BOOLEAN NOT NULL DEFAULT FALSE,
    jit_provisioning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_role VARCHAR(32) NOT NULL DEFAULT 'DISPATCHER',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_sso_domain UNIQUE (sso_domain),
    CONSTRAINT uk_company_sso_company UNIQUE (company_id),
    CONSTRAINT ck_sso_idp_type CHECK (idp_type IN ('OIDC', 'SAML2')),
    CONSTRAINT ck_sso_default_role CHECK (default_role IN ('ADMIN', 'DISPATCHER', 'DRIVER'))
);

CREATE INDEX IF NOT EXISTS idx_company_sso_domain ON company_sso_configs (sso_domain);
CREATE INDEX IF NOT EXISTS idx_company_sso_company ON company_sso_configs (company_id);

CREATE TABLE IF NOT EXISTS sso_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_email VARCHAR(128) NOT NULL,
    idp_type VARCHAR(32) NOT NULL DEFAULT 'OIDC',
    event_type VARCHAR(32) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(255),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sso_event_type CHECK (event_type IN ('LOGIN_SUCCESS', 'LOGIN_FAILED', 'USER_PROVISIONED', 'SSO_ENFORCED', 'CONFIG_UPDATED'))
);

CREATE INDEX IF NOT EXISTS idx_sso_audit_logs_company ON sso_audit_logs (company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sso_audit_logs_email ON sso_audit_logs (user_email);

-- Seed repeatable baseline enterprise SSO config for Demo Company
INSERT INTO company_sso_configs (
    id, company_id, idp_type, idp_issuer_url, client_id,
    client_secret_hash, sso_domain, enabled, enforce_sso,
    jit_provisioning_enabled, default_role, metadata
) VALUES (
    '00000000-0000-4000-9100-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'OIDC',
    'https://auth.neepo-enterprise.com/oidc',
    'neepo-care-enterprise-client-id',
    'enterprise_sso_mock_secret_hash_987',
    'neepo.com',
    TRUE,
    FALSE,
    TRUE,
    'DISPATCHER',
    '{"providerName": "Okta Enterprise Workforce", "redirectUri": "https://care.neepo.com/auth/sso/callback"}'::jsonb
) ON CONFLICT (company_id) DO NOTHING;
