CREATE UNIQUE INDEX ux_patient_tenant_email
    on patient (tenant_id, lower(email))
WHERE email IS NOT NULL