CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_companies_code UNIQUE (code)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(16) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_users_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT ck_users_role
        CHECK (role IN ('ADMIN', 'DISPATCHER', 'DRIVER')),
    CONSTRAINT ck_users_phone
        CHECK (phone ~ '^\+?[0-9]{10,15}$')
);

CREATE UNIQUE INDEX uk_users_email_normalized ON users (LOWER(email));
CREATE INDEX idx_users_company_id ON users (company_id);
CREATE INDEX idx_users_role ON users (role);
