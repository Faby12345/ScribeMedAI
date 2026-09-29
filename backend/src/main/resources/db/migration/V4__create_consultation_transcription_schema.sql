CREATE TABLE consultation (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              tenant_id UUID NOT NULL REFERENCES tenant(id),
                              patient_id UUID NOT NULL REFERENCES patient(id),
                              doctor_user_id UUID NOT NULL REFERENCES app_user(id),
                              status VARCHAR(40) NOT NULL,
                              patient_informed_at TIMESTAMPTZ,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_tenant_created_at
    ON consultation (tenant_id, created_at DESC);

CREATE INDEX idx_consultation_tenant_patient
    ON consultation (tenant_id, patient_id);

CREATE TABLE consultation_audio (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    tenant_id UUID NOT NULL REFERENCES tenant(id),
                                    consultation_id UUID NOT NULL REFERENCES consultation(id),
                                    object_key VARCHAR(500) NOT NULL,
                                    original_filename VARCHAR(255),
                                    content_type VARCHAR(100) NOT NULL,
                                    size_bytes BIGINT NOT NULL,
                                    checksum_sha256 VARCHAR(64) NOT NULL,
                                    status VARCHAR(40) NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_audio_consultation
    ON consultation_audio (tenant_id, consultation_id);

CREATE TABLE processing_job (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                tenant_id UUID NOT NULL REFERENCES tenant(id),
                                consultation_id UUID NOT NULL REFERENCES consultation(id),
                                job_type VARCHAR(50) NOT NULL,
                                status VARCHAR(40) NOT NULL,
                                attempt_count INTEGER NOT NULL DEFAULT 0,
                                max_attempts INTEGER NOT NULL DEFAULT 3,
                                next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                locked_at TIMESTAMPTZ,
                                locked_by VARCHAR(100),
                                error_code VARCHAR(100),
                                error_message_sanitized VARCHAR(500),
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                started_at TIMESTAMPTZ,
                                completed_at TIMESTAMPTZ
);

CREATE INDEX idx_processing_job_available
    ON processing_job (status, next_attempt_at, created_at);

CREATE INDEX idx_processing_job_consultation
    ON processing_job (tenant_id, consultation_id);

CREATE UNIQUE INDEX uq_processing_job_consultation_type_active
    ON processing_job (consultation_id, job_type)
    WHERE status IN ('PENDING', 'RUNNING', 'RETRY');

CREATE TABLE consultation_transcript (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         tenant_id UUID NOT NULL REFERENCES tenant(id),
                                         consultation_id UUID NOT NULL REFERENCES consultation(id),
                                         provider VARCHAR(50) NOT NULL,
                                         provider_model VARCHAR(100) NOT NULL,
                                         language VARCHAR(20),
                                         transcript_text TEXT NOT NULL,
                                         raw_provider_response JSONB,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                         CONSTRAINT uq_consultation_transcript_consultation
                                             UNIQUE (consultation_id)
);

CREATE INDEX idx_consultation_transcript_tenant_consultation
    ON consultation_transcript (tenant_id, consultation_id);
