ALTER TABLE knowledge_document
    ADD COLUMN tenant_id UUID;

ALTER TABLE knowledge_chunk
    ADD COLUMN tenant_id UUID;

DO $$
BEGIN
    IF EXISTS (
        SELECT processing_job.knowledge_document_id
        FROM processing_job
        WHERE processing_job.knowledge_document_id IS NOT NULL
        GROUP BY processing_job.knowledge_document_id
        HAVING COUNT(DISTINCT processing_job.tenant_id) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot assign knowledge document ownership: conflicting processing-job tenants exist';
    END IF;
END
$$;

UPDATE knowledge_document AS document
SET tenant_id = ownership.tenant_id
FROM (
    SELECT DISTINCT
        processing_job.knowledge_document_id,
        processing_job.tenant_id
    FROM processing_job
    WHERE processing_job.knowledge_document_id IS NOT NULL
) AS ownership
WHERE document.id = ownership.knowledge_document_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM knowledge_document
        WHERE tenant_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cannot assign knowledge document ownership: documents without processing jobs exist';
    END IF;
END
$$;

UPDATE knowledge_chunk AS chunk
SET tenant_id = document.tenant_id
FROM knowledge_document AS document
WHERE chunk.document_id = document.id;

ALTER TABLE knowledge_document
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_knowledge_document_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    ADD CONSTRAINT uq_knowledge_document_id_tenant
        UNIQUE (id, tenant_id);

ALTER TABLE knowledge_chunk
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_knowledge_chunk_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    ADD CONSTRAINT fk_knowledge_chunk_document_tenant
        FOREIGN KEY (document_id, tenant_id)
            REFERENCES knowledge_document (id, tenant_id)
            ON DELETE CASCADE;

ALTER TABLE processing_job
    ADD CONSTRAINT fk_processing_job_knowledge_document_tenant
        FOREIGN KEY (knowledge_document_id, tenant_id)
            REFERENCES knowledge_document (id, tenant_id);

ALTER TABLE knowledge_document
    DROP CONSTRAINT knowledge_document_checksum_key,
    ADD CONSTRAINT uq_knowledge_document_tenant_checksum
        UNIQUE (tenant_id, checksum);

CREATE INDEX idx_knowledge_document_tenant_status_created
    ON knowledge_document (tenant_id, status, created_at DESC);

CREATE INDEX idx_knowledge_chunk_tenant_document
    ON knowledge_chunk (tenant_id, document_id);
