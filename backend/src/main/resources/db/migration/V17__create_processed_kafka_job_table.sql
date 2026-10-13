CREATE TABLE processed_kafka_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_processed_kafka_job_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT uq_processed_kafka_job_consumer_idempotency
        UNIQUE (tenant_id, consumer_name, idempotency_key)
);

CREATE INDEX idx_processed_kafka_job_tenant_job
    ON processed_kafka_job (tenant_id, job_id);
