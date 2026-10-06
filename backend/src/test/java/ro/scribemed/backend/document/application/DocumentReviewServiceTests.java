package ro.scribemed.backend.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.ClinicalDocumentStatus;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.domain.DocumentVersionSource;
import ro.scribemed.backend.document.domain.DocumentVersionStatus;
import ro.scribemed.backend.document.dto.ApproveDocumentRequest;
import ro.scribemed.backend.document.dto.DocumentApprovalResponse;
import ro.scribemed.backend.document.dto.DocumentReviewResponse;
import ro.scribemed.backend.document.dto.SaveDocumentDraftRequest;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.document.infrastructure.DocumentVersionRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.prescribedMedication.domain.PrescribedMedication;
import ro.scribemed.backend.prescribedMedication.infrastructure.PrescribedMedicationRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.transcription.domain.ConsultationTranscript;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

class DocumentReviewServiceTests {

    private final ClinicalDocumentRepository clinicalDocumentRepository = mock(ClinicalDocumentRepository.class);
    private final DocumentVersionRepository documentVersionRepository = mock(DocumentVersionRepository.class);
    private final ConsultationTranscriptRepository transcriptRepository = mock(ConsultationTranscriptRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final PrescribedMedicationRepository prescribedMedicationRepository =
            mock(PrescribedMedicationRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final DocumentReviewService documentReviewService = new DocumentReviewService(
            clinicalDocumentRepository,
            documentVersionRepository,
            transcriptRepository,
            appUserRepository,
            prescribedMedicationRepository,
            auditService,
            new ObjectMapper()
    );

    @BeforeEach
    void saveReturnsEntityArgument() {
        when(documentVersionRepository.save(any(DocumentVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getConsultationReviewDocumentReturnsLatestDraftAndTranscriptForTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        TestDocumentData data = createDocumentData(tenantId, consultationId);

        when(clinicalDocumentRepository.findByConsultation_IdAndTenant_IdAndDocumentType(
                consultationId,
                tenantId,
                "SOAP_NOTE"
        )).thenReturn(Optional.of(data.document()));
        when(documentVersionRepository.findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                data.document().getId(),
                tenantId,
                DocumentVersionStatus.DRAFT
        )).thenReturn(Optional.of(data.version()));
        when(transcriptRepository.findByConsultation_IdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(data.transcript()));

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

    @Test
    void getConsultationReviewDocumentReturnsApprovedVersionWhenDocumentIsApproved() {
        UUID tenantId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        TestDocumentData data = createDocumentData(tenantId, consultationId);
        data.version().markApproved(data.doctor(), Instant.now());
        data.document().markApproved(data.doctor(), Instant.now());

        when(clinicalDocumentRepository.findByConsultation_IdAndTenant_IdAndDocumentType(
                consultationId,
                tenantId,
                "SOAP_NOTE"
        )).thenReturn(Optional.of(data.document()));
        when(documentVersionRepository.findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                data.document().getId(),
                tenantId,
                DocumentVersionStatus.APPROVED
        )).thenReturn(Optional.of(data.version()));
        when(transcriptRepository.findByConsultation_IdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(data.transcript()));

        DocumentReviewResponse response = documentReviewService.getConsultationReviewDocument(
                consultationId,
                tenantId
        );

        assertThat(response.documentStatus()).isEqualTo(ClinicalDocumentStatus.APPROVED);
        assertThat(response.versionStatus()).isEqualTo(DocumentVersionStatus.APPROVED);
        assertThat(response.draft().subjective()).isEqualTo("Subiectiv");
    }

    @Test
    void getConsultationReviewDocumentReturnsMedicationPlanWithoutTranscript() {
        UUID tenantId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        UUID notesId = UUID.randomUUID();
        TestDocumentData data = createDocumentData(tenantId, consultationId);
        ConsultationNotes sourceNotes = ConsultationNotes.create(
                data.tenant(),
                data.document().getConsultation(),
                data.doctor(),
                "Motiv",
                "Istoric",
                "Obiectiv",
                "Evaluare",
                "Plan"
        );
        ReflectionTestUtils.setField(sourceNotes, "id", notesId);
        ReflectionTestUtils.setField(data.version(), "sourceNotes", sourceNotes);
        PrescribedMedication medication = new PrescribedMedication(
                data.tenant(),
                sourceNotes,
                0,
                "CIM-1",
                "Paracetamol",
                "Paracetamolum",
                "COMPRIMAT",
                "500 mg",
                "PRF",
                "500 mg",
                "ORAL",
                "De două ori pe zi",
                "7 zile",
                "14 comprimate",
                "După masă",
                null
        );
        UUID medicationId = UUID.randomUUID();
        ReflectionTestUtils.setField(medication, "id", medicationId);

        when(clinicalDocumentRepository.findByConsultation_IdAndTenant_IdAndDocumentType(
                consultationId,
                tenantId,
                "SOAP_NOTE"
        )).thenReturn(Optional.of(data.document()));
        when(documentVersionRepository.findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                data.document().getId(),
                tenantId,
                DocumentVersionStatus.DRAFT
        )).thenReturn(Optional.of(data.version()));
        when(transcriptRepository.findByConsultation_IdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.empty());
        when(prescribedMedicationRepository.findByConsultationNotes_IdAndTenant_IdOrderByPositionAsc(
                notesId,
                tenantId
        )).thenReturn(List.of(medication));

        DocumentReviewResponse response = documentReviewService.getConsultationReviewDocument(
                consultationId,
                tenantId
        );

        assertThat(response.transcript()).isNull();
        assertThat(response.medications()).hasSize(1);
        assertThat(response.medications().getFirst().id()).isEqualTo(medicationId);
        assertThat(response.medications().getFirst().cimCode()).isEqualTo("CIM-1");
        assertThat(response.medications().getFirst().commercialName()).isEqualTo("Paracetamol");
        assertThat(response.medications().getFirst().dose()).isEqualTo("500 mg");
    }

    @Test
    void saveDraftCreatesDoctorEditedVersionAndSupersedesExistingDraft() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        UUID newVersionId = UUID.randomUUID();
        TestDocumentData data = createDocumentData(tenantId, consultationId);
        SaveDocumentDraftRequest request = new SaveDocumentDraftRequest(
                new SaveDocumentDraftRequest.SoapDraftRequest(
                        "Subiectiv editat",
                        "Obiectiv editat",
                        "Evaluare editata",
                        "Plan editat"
                ),
                List.of("REVIEWED_DOSAGE")
        );

        when(clinicalDocumentRepository.findByIdAndTenant_Id(data.document().getId(), tenantId))
                .thenReturn(Optional.of(data.document()));
        when(appUserRepository.findByIdAndTenant_Id(userId, tenantId))
                .thenReturn(Optional.of(data.doctor()));
        when(documentVersionRepository.findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                data.document().getId(),
                tenantId,
                DocumentVersionStatus.DRAFT
        )).thenReturn(Optional.of(data.version()));
        when(documentVersionRepository.findByDocument_IdAndTenant_IdOrderByVersionNumberDesc(
                data.document().getId(),
                tenantId
        )).thenReturn(List.of(data.version()));
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion savedVersion = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedVersion, "id", newVersionId);
            return savedVersion;
        });
        when(transcriptRepository.findByConsultation_IdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(data.transcript()));

        DocumentReviewResponse response = documentReviewService.saveDraft(
                data.document().getId(),
                tenantId,
                userId,
                request
        );

        assertThat(data.version().getStatus()).isEqualTo(DocumentVersionStatus.SUPERSEDED);
        assertThat(response.versionId()).isEqualTo(newVersionId);
        assertThat(response.versionNumber()).isEqualTo(2);
        assertThat(response.source()).isEqualTo(DocumentVersionSource.DOCTOR_EDITED);
        assertThat(response.draft().subjective()).isEqualTo("Subiectiv editat");
        assertThat(response.reviewFlags()).containsExactly("REVIEWED_DOSAGE");
        verify(auditService).record(
                data.tenant(),
                data.doctor(),
                "DOCUMENT_DRAFT_SAVED",
                "CLINICAL_DOCUMENT",
                data.document().getId(),
                java.util.Map.of("versionNumber", 2, "source", "DOCTOR_EDITED")
        );
    }

    @Test
    void approveDocumentApprovesCurrentDraftVersion() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        TestDocumentData data = createDocumentData(tenantId, consultationId);

        when(clinicalDocumentRepository.findByIdAndTenant_Id(data.document().getId(), tenantId))
                .thenReturn(Optional.of(data.document()));
        when(appUserRepository.findByIdAndTenant_Id(userId, tenantId))
                .thenReturn(Optional.of(data.doctor()));
        when(documentVersionRepository.findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                data.document().getId(),
                tenantId,
                DocumentVersionStatus.DRAFT
        )).thenReturn(Optional.of(data.version()));

        DocumentApprovalResponse response = documentReviewService.approveDocument(
                data.document().getId(),
                tenantId,
                userId,
                new ApproveDocumentRequest(data.version().getId(), true)
        );

        assertThat(response.documentStatus()).isEqualTo(ClinicalDocumentStatus.APPROVED);
        assertThat(response.versionStatus()).isEqualTo(DocumentVersionStatus.APPROVED);
        assertThat(response.approvedAt()).isNotNull();
        assertThat(data.document().getStatus()).isEqualTo(ClinicalDocumentStatus.APPROVED);
        assertThat(data.version().getStatus()).isEqualTo(DocumentVersionStatus.APPROVED);
        verify(auditService).record(
                data.tenant(),
                data.doctor(),
                "DOCUMENT_APPROVED",
                "CLINICAL_DOCUMENT",
                data.document().getId(),
                java.util.Map.of("versionNumber", 1)
        );
    }

    @Test
    void approveDocumentRejectsStaleDraftVersion() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        TestDocumentData data = createDocumentData(tenantId, consultationId);

        when(clinicalDocumentRepository.findByIdAndTenant_Id(data.document().getId(), tenantId))
                .thenReturn(Optional.of(data.document()));
        when(appUserRepository.findByIdAndTenant_Id(userId, tenantId))
                .thenReturn(Optional.of(data.doctor()));
        when(documentVersionRepository.findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                data.document().getId(),
                tenantId,
                DocumentVersionStatus.DRAFT
        )).thenReturn(Optional.of(data.version()));

        assertThatThrownBy(() -> documentReviewService.approveDocument(
                data.document().getId(),
                tenantId,
                userId,
                new ApproveDocumentRequest(UUID.randomUUID(), true)
        )).isInstanceOf(DocumentStateException.class);

        assertThat(data.document().getStatus()).isEqualTo(ClinicalDocumentStatus.DRAFT);
        assertThat(data.version().getStatus()).isEqualTo(DocumentVersionStatus.DRAFT);
    }

    private TestDocumentData createDocumentData(UUID tenantId, UUID consultationId) {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        ReflectionTestUtils.setField(doctor, "id", UUID.randomUUID());
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        ReflectionTestUtils.setField(consultation, "id", consultationId);
        ClinicalDocument document = new ClinicalDocument(tenant, consultation, "SOAP_NOTE");
        ReflectionTestUtils.setField(document, "id", documentId);
        ReflectionTestUtils.setField(document, "currentVersionNumber", 1);
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
        ReflectionTestUtils.setField(version, "id", versionId);
        ConsultationTranscript transcript = new ConsultationTranscript(
                tenant,
                consultation,
                "deepgram",
                "nova-3",
                "ro",
                "Text transcriere",
                "{}"
        );

        return new TestDocumentData(tenant, doctor, document, version, transcript);
    }

    private record TestDocumentData(
            Tenant tenant,
            AppUser doctor,
            ClinicalDocument document,
            DocumentVersion version,
            ConsultationTranscript transcript
    ) {
    }
}
