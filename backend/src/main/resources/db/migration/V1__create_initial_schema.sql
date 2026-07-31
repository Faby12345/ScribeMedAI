CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenant (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name VARCHAR(200) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE app_user (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          tenant_id UUID NOT NULL REFERENCES tenant(id),
                          email VARCHAR(320) NOT NULL,
                          password_hash VARCHAR(255) NOT NULL,
                          display_name VARCHAR(200) NOT NULL,
                          role VARCHAR(50) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                          CONSTRAINT uq_app_user_tenant_email
                              UNIQUE (tenant_id, email)
);