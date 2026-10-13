ALTER TABLE consultation
    ADD CONSTRAINT uq_consultation_id_tenant
        UNIQUE (id, tenant_id);

ALTER TABLE consultation_transcript
    ADD CONSTRAINT uq_consultation_transcript_id_consultation_tenant
        UNIQUE (id, consultation_id, tenant_id);

ALTER TABLE document_version
    ADD CONSTRAINT uq_document_version_id_consultation_tenant
        UNIQUE (id, consultation_id, tenant_id);

ALTER TABLE app_user
    ADD CONSTRAINT uq_app_user_id_tenant
        UNIQUE (id, tenant_id);

CREATE TABLE medication_draft_candidate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    consultation_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    transcript_id UUID NOT NULL,
    position INTEGER NOT NULL,
    mention_type VARCHAR(30) NOT NULL,
    review_status VARCHAR(40) NOT NULL,
    extracted_name VARCHAR(255) NOT NULL,
    catalog_cim_code VARCHAR(255),
    dose VARCHAR(255),
    administration_route VARCHAR(100),
    frequency VARCHAR(255),
    duration VARCHAR(255),
    quantity VARCHAR(255),
    instructions TEXT,
    source_segment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_medication_draft_candidate_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_medication_draft_candidate_consultation_tenant
        FOREIGN KEY (consultation_id, tenant_id)
            REFERENCES consultation (id, tenant_id),
    CONSTRAINT fk_medication_draft_candidate_document_version_tenant
        FOREIGN KEY (document_version_id, consultation_id, tenant_id)
            REFERENCES document_version (id, consultation_id, tenant_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_medication_draft_candidate_transcript_tenant
        FOREIGN KEY (transcript_id, consultation_id, tenant_id)
            REFERENCES consultation_transcript (id, consultation_id, tenant_id),
    CONSTRAINT fk_medication_draft_candidate_catalog_medication
        FOREIGN KEY (catalog_cim_code) REFERENCES medication (cim_code),
    CONSTRAINT fk_medication_draft_candidate_reviewer_tenant
        FOREIGN KEY (reviewed_by_user_id, tenant_id)
            REFERENCES app_user (id, tenant_id),
    CONSTRAINT ck_medication_draft_candidate_position_non_negative
        CHECK (position >= 0),
    CONSTRAINT ck_medication_draft_candidate_mention_type
        CHECK (mention_type IN ('CURRENT', 'START', 'CHANGE', 'STOP', 'UNCERTAIN')),
    CONSTRAINT ck_medication_draft_candidate_review_status
        CHECK (review_status IN (
            'PENDING_REVIEW',
            'ACCEPTED_AS_IS',
            'ACCEPTED_WITH_CHANGES',
            'REJECTED',
            'UNRESOLVED'
        )),
    CONSTRAINT ck_medication_draft_candidate_review_metadata
        CHECK (
            (review_status = 'PENDING_REVIEW'
                AND reviewed_by_user_id IS NULL
                AND reviewed_at IS NULL)
            OR
            (review_status <> 'PENDING_REVIEW'
                AND reviewed_by_user_id IS NOT NULL
                AND reviewed_at IS NOT NULL)
        ),
    CONSTRAINT ck_medication_draft_candidate_source_segments_array
        CHECK (jsonb_typeof(source_segment_ids) = 'array'),
    CONSTRAINT uq_medication_draft_candidate_version_position
        UNIQUE (tenant_id, document_version_id, position)
);

CREATE INDEX idx_medication_draft_candidate_tenant_consultation_status
    ON medication_draft_candidate (tenant_id, consultation_id, review_status);

CREATE INDEX idx_medication_draft_candidate_tenant_transcript
    ON medication_draft_candidate (tenant_id, transcript_id);

CREATE INDEX idx_medication_draft_candidate_tenant_catalog_cim
    ON medication_draft_candidate (tenant_id, catalog_cim_code)
    WHERE catalog_cim_code IS NOT NULL;
