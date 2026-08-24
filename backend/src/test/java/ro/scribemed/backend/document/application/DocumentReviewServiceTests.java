package ro.scribemed.backend.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.domain.DocumentVersionSource;
import ro.scribemed.backend.document.domain.DocumentVersionStatus;
import ro.scribemed.backend.document.dto.DocumentReviewResponse;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.document.infrastructure.DocumentVersionRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.transcription.domain.ConsultationTranscript;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

class DocumentReviewServiceTests {

    private final ClinicalDocumentRepository clinicalDocumentRepository = mock(ClinicalDocumentRepository.class);
    private final DocumentVersionRepository documentVersionRepository = mock(DocumentVersionRepository.class);
    private final ConsultationTranscriptRepository transcriptRepository = mock(ConsultationTranscriptRepository.class);
    private final DocumentReviewService documentReviewService = new DocumentReviewService(
            clinicalDocumentRepository,
            documentVersionRepository,
            transcriptRepository,
            new ObjectMapper()
    );

    @Test
    void getConsultationReviewDocumentReturnsLatestDraftAndTranscriptForTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        ClinicalDocument document = new ClinicalDocument(tenant, consultation, "SOAP_NOTE");
        DocumentVersion version = new DocumentVersion(
                tenant,
                document,
                consultation,
                null,
                1,
                DocumentVersionSource.AI_GENERATED,
                "Subiectiv",
                "Obiectiv",
                "Evaluare",
                "Plan",
                "[\"DOSAGE_REVIEW\",\"NEGATION_REVIEW\"]",
                "huggingface",
                "Qwen/Qwen3-32B:cheapest",
                "clinical-note-soap-from-transcript-v1",
                "soap-v1",
                doctor
        );
        ConsultationTranscript transcript = new ConsultationTranscript(
                tenant,
                consultation,
                "deepgram",
                "nova-3",
                "ro",
                "Text transcriere",
                "{}"
        );

        when(clinicalDocumentRepository.findByConsultation_IdAndTenant_IdAndDocumentType(
                consultationId,
                tenantId,
                "SOAP_NOTE"
        )).thenReturn(Optional.of(document));
        when(documentVersionRepository.findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                document.getId(),
                tenantId,
                DocumentVersionStatus.DRAFT
        )).thenReturn(Optional.of(version));
        when(transcriptRepository.findByConsultation_IdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(transcript));

        DocumentReviewResponse response = documentReviewService.getConsultationReviewDocument(
                consultationId,
                tenantId
        );

        assertThat(response.documentType()).isEqualTo("SOAP_NOTE");
        assertThat(response.draft().subjective()).isEqualTo("Subiectiv");
        assertThat(response.reviewFlags()).containsExactly("DOSAGE_REVIEW", "NEGATION_REVIEW");
        assertThat(response.aiProvider()).isEqualTo("huggingface");
        assertThat(response.transcript().transcriptText()).isEqualTo("Text transcriere");
    }

    @Test
    void getConsultationReviewDocumentRequiresTenantOwnedDocument() {
        UUID tenantId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();

        when(clinicalDocumentRepository.findByConsultation_IdAndTenant_IdAndDocumentType(
                consultationId,
                tenantId,
                "SOAP_NOTE"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentReviewService.getConsultationReviewDocument(
                consultationId,
                tenantId
        )).isInstanceOf(EntityNotFoundException.class);
    }
}
