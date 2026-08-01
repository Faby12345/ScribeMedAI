CREATE TABLE patient (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         tenant_id UUID NOT NULL REFERENCES tenant(id),
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         birth_date DATE,
                         sex VARCHAR(20),
                         phone VARCHAR(50),
                         email VARCHAR(320),
                         status VARCHAR(30) NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_patient_tenant_id
    ON patient (tenant_id);

CREATE INDEX idx_patient_tenant_name
    ON patient (tenant_id, last_name, first_name);
