-- Shows raw consultation notes and their generated document versions in sequence.
-- Optional filters:
--   Replace NULL::uuid in params with a tenant_id or consultation_id to narrow the output.

WITH params AS (
    SELECT
        NULL::uuid AS tenant_id,
        NULL::uuid AS consultation_id
),
raw_notes AS (
    SELECT
        cn.tenant_id,
        cn.consultation_id,
        cn.id AS notes_id,
        cn.created_at AS notes_created_at,
        cn.created_by_user_id AS notes_created_by_user_id,
        cn.reason AS raw_reason,
        cn.history AS raw_history,
        cn.objective AS raw_objective,
        cn.assessment AS raw_assessment,
        cn.plan AS raw_plan
    FROM consultation_notes cn
    CROSS JOIN params p
    WHERE (p.tenant_id IS NULL OR cn.tenant_id = p.tenant_id)
      AND (p.consultation_id IS NULL OR cn.consultation_id = p.consultation_id)
),
rows_to_display AS (
    SELECT
        rn.tenant_id,
        rn.consultation_id,
        rn.notes_id,
        rn.notes_created_at,
        rn.notes_created_by_user_id,
        0 AS row_order,
        NULL::integer AS version_number,
        'RAW_NOTES' AS row_type,
        NULL::uuid AS document_id,
        NULL::uuid AS document_version_id,
        NULL::varchar AS document_status,
        NULL::varchar AS version_status,
        NULL::varchar AS version_source,
        rn.raw_reason,
        rn.raw_history,
        rn.raw_objective,
        rn.raw_assessment,
        rn.raw_plan,
        NULL::text AS subjective,
        NULL::text AS generated_objective,
        NULL::text AS generated_assessment,
        NULL::text AS generated_plan,
        NULL::jsonb AS review_flags,
        NULL::varchar AS ai_provider,
        NULL::varchar AS ai_model,
        NULL::varchar AS prompt_version,
        NULL::varchar AS template_version,
        NULL::timestamptz AS document_version_created_at,
        NULL::timestamptz AS approved_at
    FROM raw_notes rn

    UNION ALL

    SELECT
        rn.tenant_id,
        rn.consultation_id,
        rn.notes_id,
        rn.notes_created_at,
        rn.notes_created_by_user_id,
        1 AS row_order,
        dv.version_number,
        'DOCUMENT_VERSION' AS row_type,
        cd.id AS document_id,
        dv.id AS document_version_id,
        cd.status AS document_status,
        dv.status AS version_status,
        dv.source AS version_source,
        rn.raw_reason,
        rn.raw_history,
        rn.raw_objective,
        rn.raw_assessment,
        rn.raw_plan,
        dv.subjective,
        dv.objective AS generated_objective,
        dv.assessment AS generated_assessment,
        dv.plan AS generated_plan,
        dv.review_flags,
        dv.ai_provider,
        dv.ai_model,
        dv.prompt_version,
        dv.template_version,
        dv.created_at AS document_version_created_at,
        dv.approved_at
    FROM raw_notes rn
    JOIN document_version source_version
        ON source_version.tenant_id = rn.tenant_id
       AND source_version.source_notes_id = rn.notes_id
    JOIN clinical_document cd
        ON cd.id = source_version.document_id
       AND cd.tenant_id = source_version.tenant_id
    JOIN document_version dv
        ON dv.tenant_id = cd.tenant_id
       AND dv.document_id = cd.id
)
SELECT
    tenant_id,
    consultation_id,
    notes_id,
    notes_created_at,
    notes_created_by_user_id,
    row_type,
    document_id,
    document_version_id,
    version_number,
    document_status,
    version_status,
    version_source,
    raw_reason,
    raw_history,
    raw_objective,
    raw_assessment,
    raw_plan,
    subjective AS generated_subjective,
    generated_objective,
    generated_assessment,
    generated_plan,
    review_flags,
    ai_provider,
    ai_model,
    prompt_version,
    template_version,
    document_version_created_at,
    approved_at
FROM rows_to_display
ORDER BY
    tenant_id,
    consultation_id,
    notes_created_at,
    notes_id,
    row_order,
    version_number NULLS FIRST,
    document_version_created_at NULLS FIRST;
