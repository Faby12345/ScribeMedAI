package ro.scribemed.backend.medication.infrastructure.importdata.application;

import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ro.scribemed.backend.medication.infrastructure.importdata.configuration.MedicationImportRow;

import java.util.List;

@Repository
public class MedicationJdbcWriter {

    private static final String UPSERT_SQL = """
            INSERT INTO medication (
                cim_code,
                commercial_name,
                active_substance,
                pharmaceutical_form,
                concentration,
                app_manufacturer,
                app_holder,
                atc_code,
                therapeutic_action,
                prescription_type,
                app_packaging_authorization,
                packaging,
                packaging_volume,
                packaging_validity,
                centralized_pending_romanian_decision,
                temporary_circulation,
                centralized_authorized,
                authorization_suspended,
                has_additional_information,
                source_updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (cim_code)
            DO UPDATE SET
                commercial_name = EXCLUDED.commercial_name,
                active_substance = EXCLUDED.active_substance,
                pharmaceutical_form = EXCLUDED.pharmaceutical_form,
                concentration = EXCLUDED.concentration,
                app_manufacturer = EXCLUDED.app_manufacturer,
                app_holder = EXCLUDED.app_holder,
                atc_code = EXCLUDED.atc_code,
                therapeutic_action = EXCLUDED.therapeutic_action,
                prescription_type = EXCLUDED.prescription_type,
                app_packaging_authorization =
                    EXCLUDED.app_packaging_authorization,
                packaging = EXCLUDED.packaging,
                packaging_volume = EXCLUDED.packaging_volume,
                packaging_validity = EXCLUDED.packaging_validity,
                centralized_pending_romanian_decision =
                    EXCLUDED.centralized_pending_romanian_decision,
                temporary_circulation =
                    EXCLUDED.temporary_circulation,
                centralized_authorized =
                    EXCLUDED.centralized_authorized,
                authorization_suspended =
                    EXCLUDED.authorization_suspended,
                has_additional_information =
                    EXCLUDED.has_additional_information,
                source_updated_at =
                    EXCLUDED.source_updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public MedicationJdbcWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void write(List<MedicationImportRow> rows) {
        jdbcTemplate.batchUpdate(
                UPSERT_SQL,
                rows,
                rows.size(),
                (statement, row) -> {
                    statement.setString(1, row.cimCode());
                    statement.setString(2, row.commercialName());
                    statement.setString(3, row.activeSubstance());
                    statement.setString(4, row.pharmaceuticalForm());
                    statement.setString(5, row.concentration());
                    statement.setString(6, row.appManufacturer());
                    statement.setString(7, row.appHolder());
                    statement.setString(8, row.atcCode());
                    statement.setString(9, row.therapeuticAction());
                    statement.setString(10, row.prescriptionType());
                    statement.setString(11, row.appPackagingAuthorization());
                    statement.setString(12, row.packaging());
                    statement.setString(13, row.packagingVolume());
                    statement.setString(14, row.packagingValidity());

                    statement.setBoolean(
                            15,
                            row.centralizedPendingRomanianDecision()
                    );
                    statement.setBoolean(16, row.temporaryCirculation());
                    statement.setBoolean(17, row.centralizedAuthorized());
                    statement.setBoolean(18, row.authorizationSuspended());
                    statement.setBoolean(19, row.hasAdditionalInformation());

                    statement.setObject(20, row.sourceUpdatedAt());
                }
        );
    }
}
