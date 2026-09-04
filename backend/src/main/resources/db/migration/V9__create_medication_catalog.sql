CREATE TABLE medication (
    cim_code VARCHAR(255) PRIMARY KEY,
    commercial_name VARCHAR(255),
    active_substance VARCHAR(255),
    pharmaceutical_form VARCHAR(255),
    concentration VARCHAR(255),
    app_manufacturer VARCHAR(255),
    app_holder VARCHAR(255),
    atc_code VARCHAR(255),
    therapeutic_action VARCHAR(255),
    prescription_type VARCHAR(255),
    app_packaging_authorization VARCHAR(255),
    packaging VARCHAR(255),
    packaging_volume VARCHAR(255),
    packaging_validity VARCHAR(255),
    centralized_pending_romanian_decision BOOLEAN NOT NULL DEFAULT FALSE,
    temporary_circulation BOOLEAN NOT NULL DEFAULT FALSE,
    centralized_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    authorization_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    has_additional_information BOOLEAN NOT NULL DEFAULT FALSE,
    source_updated_at DATE
);

CREATE INDEX idx_medication_commercial_name
    ON medication (commercial_name);

CREATE INDEX idx_medication_active_substance
    ON medication (active_substance);

CREATE INDEX idx_medication_atc_code
    ON medication (atc_code);
