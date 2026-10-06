ALTER TABLE consultation_notes
    ADD CONSTRAINT uq_consultation_notes_id_tenant
        UNIQUE (id, tenant_id);

CREATE TABLE prescribed_medication (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    consultation_notes_id UUID NOT NULL,
    position INTEGER NOT NULL,
    catalog_cim_code VARCHAR(255) NOT NULL,
    commercial_name_snapshot VARCHAR(255),
    active_substance_snapshot VARCHAR(255),
    pharmaceutical_form_snapshot VARCHAR(255),
    concentration_snapshot VARCHAR(255),
    prescription_type_snapshot VARCHAR(255),
    dose VARCHAR(255) NOT NULL,
    administration_route VARCHAR(100) NOT NULL,
    frequency VARCHAR(255) NOT NULL,
    duration VARCHAR(255) NOT NULL,
    quantity VARCHAR(255),
    instructions TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_prescribed_medication_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_prescribed_medication_consultation_notes_tenant
        FOREIGN KEY (consultation_notes_id, tenant_id)
            REFERENCES consultation_notes (id, tenant_id)
            ON DELETE CASCADE,
    CONSTRAINT ck_prescribed_medication_position_non_negative
        CHECK (position >= 0),
    CONSTRAINT uq_prescribed_medication_note_position
        UNIQUE (tenant_id, consultation_notes_id, position)
);

CREATE INDEX idx_prescribed_medication_tenant_consultation_notes
    ON prescribed_medication (tenant_id, consultation_notes_id);

CREATE INDEX idx_prescribed_medication_tenant_catalog_cim
    ON prescribed_medication (tenant_id, catalog_cim_code);
