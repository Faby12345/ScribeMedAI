ALTER TABLE knowledge_document
    ADD COLUMN object_key VARCHAR(1000);

-- Documents created before object keys were persisted cannot be associated
-- reliably with their stored file and must be uploaded again.
UPDATE knowledge_document
SET object_key = 'legacy/unavailable/' || id::text || '.pdf'
WHERE object_key IS NULL;

ALTER TABLE knowledge_document
    ALTER COLUMN object_key SET NOT NULL;

ALTER TABLE knowledge_document
    ADD CONSTRAINT uq_knowledge_document_object_key UNIQUE (object_key);
