CREATE TABLE clinical_document (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   tenant_id UUID NOT NULL REFERENCES tenant(id),
                                   consultation_id UUID NOT NULL REFERENCES consultation(id),
                                   document_type VARCHAR(50) NOT NULL,
                                   status VARCHAR(40) NOT NULL,
                                   current_version_number INTEGER NOT NULL DEFAULT 0,
                                   approved_by_user_id UUID REFERENCES app_user(id),
                                   approved_at TIMESTAMPTZ,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                   CONSTRAINT uq_clinical_document_tenant_consultation_type
                                       UNIQUE (tenant_id, consultation_id, document_type)
);

CREATE INDEX idx_clinical_document_tenant_consultation
    ON clinical_document (tenant_id, consultation_id);

CREATE TABLE document_version (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  tenant_id UUID NOT NULL REFERENCES tenant(id),
                                  document_id UUID NOT NULL REFERENCES clinical_document(id),
                                  consultation_id UUID NOT NULL REFERENCES consultation(id),
                                  source_notes_id UUID REFERENCES consultation_notes(id),
                                  version_number INTEGER NOT NULL,
                                  status VARCHAR(40) NOT NULL,
                                  source VARCHAR(40) NOT NULL,
                                  subjective TEXT,
                                  objective TEXT,
                                  assessment TEXT,
                                  plan TEXT,
                                  review_flags JSONB NOT NULL DEFAULT '[]'::jsonb,
                                  ai_provider VARCHAR(100),
                                  ai_model VARCHAR(200),
                                  prompt_version VARCHAR(100),
                                  template_version VARCHAR(100),
                                  created_by_user_id UUID REFERENCES app_user(id),
                                  approved_by_user_id UUID REFERENCES app_user(id),
                                  approved_at TIMESTAMPTZ,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                  CONSTRAINT uq_document_version_tenant_document_number
                                      UNIQUE (tenant_id, document_id, version_number),
                                  CONSTRAINT uq_document_version_tenant_source_notes
                                      UNIQUE (tenant_id, source_notes_id)
);

CREATE INDEX idx_document_version_tenant_document
    ON document_version (tenant_id, document_id, version_number DESC);

CREATE INDEX idx_document_version_tenant_consultation
    ON document_version (tenant_id, consultation_id, created_at DESC);

ALTER TABLE processing_job
    ADD COLUMN source_notes_id UUID REFERENCES consultation_notes(id);

CREATE INDEX idx_processing_job_tenant_source_notes
    ON processing_job (tenant_id, source_notes_id);
