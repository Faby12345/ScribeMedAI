ALTER TABLE processing_job
    ALTER COLUMN consultation_id DROP NOT NULL;

ALTER TABLE processing_job
    ADD COLUMN knowledge_document_id UUID
        REFERENCES knowledge_document (id),
    ADD COLUMN target_type VARCHAR(40);

UPDATE processing_job
SET target_type = CASE
    WHEN knowledge_document_id IS NOT NULL THEN 'KNOWLEDGE_DOCUMENT'
    ELSE 'CONSULTATION'
END;

ALTER TABLE processing_job
    ALTER COLUMN target_type SET NOT NULL;

ALTER TABLE processing_job
    ADD CONSTRAINT chk_processing_job_target_type
        CHECK (target_type IN ('CONSULTATION', 'KNOWLEDGE_DOCUMENT')),
    ADD CONSTRAINT chk_processing_job_target
        CHECK (
            (
                target_type = 'CONSULTATION'
                AND consultation_id IS NOT NULL
                AND knowledge_document_id IS NULL
            )
            OR
            (
                target_type = 'KNOWLEDGE_DOCUMENT'
                AND consultation_id IS NULL
                AND knowledge_document_id IS NOT NULL
            )
        );

CREATE INDEX idx_processing_job_tenant_knowledge_document
    ON processing_job (tenant_id, knowledge_document_id);

CREATE UNIQUE INDEX uq_processing_job_knowledge_document_type_active
    ON processing_job (knowledge_document_id, job_type)
    WHERE knowledge_document_id IS NOT NULL
      AND status IN ('PENDING', 'RUNNING', 'RETRY');
