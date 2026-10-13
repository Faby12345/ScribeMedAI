package ro.scribemed.backend.medicationdraft.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ro.scribemed.backend.medicationdraft.domain.MedicationDraftCandidate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MedicationDraftCandidateRepositoryIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18")
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("scribemed_medication_draft_test")
            .withUsername("test")
            .withPassword("test");

    private final JdbcTemplate jdbcTemplate;
    private final MedicationDraftCandidateRepository repository;

    MedicationDraftCandidateRepositoryIntegrationTests(
            JdbcTemplate jdbcTemplate,
            MedicationDraftCandidateRepository repository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
    }

    @Test
    void returnsCandidatesInPositionOrderOnlyForTheRequestedTenant() {
        Fixture fixture = createFixture();
        UUID otherTenantId = insertTenant("Clinică secundară");
        insertCandidate(fixture, fixture.tenantId(), UUID.randomUUID(), 1, "Ibuprofen");
        insertCandidate(fixture, fixture.tenantId(), UUID.randomUUID(), 0, "Paracetamol");

        List<MedicationDraftCandidate> result =
                repository.findByDocumentVersion_IdAndTenant_IdOrderByPositionAsc(
                        fixture.documentVersionId(),
                        fixture.tenantId()
                );

        assertThat(result)
                .extracting(MedicationDraftCandidate::getExtractedName)
                .containsExactly("Paracetamol", "Ibuprofen");
        assertThat(repository.findByDocumentVersion_IdAndTenant_IdOrderByPositionAsc(
                fixture.documentVersionId(),
                otherTenantId
        )).isEmpty();
    }

    @Test
    void databaseRejectsCandidateWhoseTenantDoesNotOwnTheConsultation() {
        Fixture fixture = createFixture();
        UUID otherTenantId = insertTenant("Clinică secundară");

        assertThatThrownBy(() -> insertCandidate(
                fixture,
                otherTenantId,
                UUID.randomUUID(),
                0,
                "Paracetamol"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Fixture createFixture() {
        UUID tenantId = insertTenant("Clinică test");
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        UUID transcriptId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID documentVersionId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO app_user (
                    id, tenant_id, email, password_hash, display_name, role, status
                ) VALUES (?, ?, ?, 'hash', 'Medic test', 'DOCTOR', 'ACTIVE')
                """, doctorId, tenantId, doctorId + "@example.test");
        jdbcTemplate.update("""
                INSERT INTO patient (
                    id, tenant_id, first_name, last_name, status
                ) VALUES (?, ?, 'Pacient', 'Test', 'ACTIVE')
                """, patientId, tenantId);
        jdbcTemplate.update("""
                INSERT INTO consultation (
                    id, tenant_id, patient_id, doctor_user_id, status
                ) VALUES (?, ?, ?, ?, 'NOTES_READY')
                """, consultationId, tenantId, patientId, doctorId);
        jdbcTemplate.update("""
                INSERT INTO consultation_transcript (
                    id, tenant_id, consultation_id, provider, provider_model,
                    language, transcript_text
                ) VALUES (?, ?, ?, 'test', 'test-model', 'ro', 'Conținut test')
                """, transcriptId, tenantId, consultationId);
        jdbcTemplate.update("""
                INSERT INTO clinical_document (
                    id, tenant_id, consultation_id, document_type, status,
                    current_version_number
                ) VALUES (?, ?, ?, 'SOAP_NOTE', 'DRAFT', 1)
                """, documentId, tenantId, consultationId);
        jdbcTemplate.update("""
                INSERT INTO document_version (
                    id, tenant_id, document_id, consultation_id, version_number,
                    status, source, review_flags
                ) VALUES (?, ?, ?, ?, 1, 'DRAFT', 'AI_GENERATED', '[]'::jsonb)
                """, documentVersionId, tenantId, documentId, consultationId);

        return new Fixture(tenantId, consultationId, transcriptId, documentVersionId);
    }

    private UUID insertTenant(String name) {
        UUID tenantId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tenant (id, name, status) VALUES (?, ?, 'ACTIVE')",
                tenantId,
                name
        );
        return tenantId;
    }

    private void insertCandidate(
            Fixture fixture,
            UUID tenantId,
            UUID candidateId,
            int position,
            String extractedName
    ) {
        jdbcTemplate.update("""
                INSERT INTO medication_draft_candidate (
                    id, tenant_id, consultation_id, document_version_id,
                    transcript_id, position, mention_type, review_status,
                    extracted_name, source_segment_ids
                ) VALUES (?, ?, ?, ?, ?, ?, 'START', 'PENDING_REVIEW', ?, '["seg-1"]'::jsonb)
                """,
                candidateId,
                tenantId,
                fixture.consultationId(),
                fixture.documentVersionId(),
                fixture.transcriptId(),
                position,
                extractedName
        );
    }

    private record Fixture(
            UUID tenantId,
            UUID consultationId,
            UUID transcriptId,
            UUID documentVersionId
    ) {
    }
}
