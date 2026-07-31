ALTER TABLE tenant
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE app_user
    ADD COLUMN last_login_at TIMESTAMPTZ;

CREATE INDEX idx_app_user_tenant_id
    ON app_user (tenant_id);

CREATE TABLE user_session (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              tenant_id UUID NOT NULL REFERENCES tenant(id),
                              user_id UUID NOT NULL REFERENCES app_user(id),
                              session_token_hash VARCHAR(255) NOT NULL,
                              expires_at TIMESTAMPTZ NOT NULL,
                              revoked_at TIMESTAMPTZ,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                              CONSTRAINT uq_user_session_token_hash
                                  UNIQUE (session_token_hash)
);

CREATE INDEX idx_user_session_user_id
    ON user_session (user_id);

CREATE INDEX idx_user_session_tenant_id
    ON user_session (tenant_id);

CREATE INDEX idx_user_session_active_lookup
    ON user_session (session_token_hash, expires_at)
    WHERE revoked_at IS NULL;

CREATE TABLE audit_event (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             tenant_id UUID NOT NULL REFERENCES tenant(id),
                             actor_user_id UUID REFERENCES app_user(id),
                             event_type VARCHAR(100) NOT NULL,
                             resource_type VARCHAR(100),
                             resource_id UUID,
                             occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_audit_event_tenant_occurred_at
    ON audit_event (tenant_id, occurred_at DESC);

CREATE INDEX idx_audit_event_actor_user_id
    ON audit_event (actor_user_id);

CREATE INDEX idx_audit_event_resource
    ON audit_event (tenant_id, resource_type, resource_id);
