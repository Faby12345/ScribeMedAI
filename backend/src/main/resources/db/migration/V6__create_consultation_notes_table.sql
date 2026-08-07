CREATE TABLE consultation_notes (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    tenant_id UUID NOT NULL REFERENCES tenant(id),
                                    consultation_id UUID NOT NULL REFERENCES consultation(id),
                                    created_by_user_id UUID NOT NULL REFERENCES app_user(id),
                                    reason TEXT,
                                    history TEXT,
                                    objective TEXT,
                                    assessment TEXT,
                                    plan TEXT,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_notes_tenant_consultation_created_at
    ON consultation_notes (tenant_id, consultation_id, created_at DESC);

CREATE INDEX idx_consultation_notes_tenant_created_by
    ON consultation_notes (tenant_id, created_by_user_id);
