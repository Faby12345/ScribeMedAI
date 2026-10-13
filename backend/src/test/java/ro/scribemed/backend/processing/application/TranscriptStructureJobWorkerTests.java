package ro.scribemed.backend.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;
import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationProvider;
import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationResult;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationStatus;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.document.infrastructure.DocumentVersionRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.medicationdraft.domain.MedicationDraftCandidate;
import ro.scribemed.backend.medicationdraft.domain.MedicationMentionType;
import ro.scribemed.backend.medicationdraft.domain.MedicationReviewStatus;
import ro.scribemed.backend.medicationdraft.infrastructure.MedicationDraftCandidateRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobStatus;
import ro.scribemed.backend.processing.domain.ProcessingJobType;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.transcription.domain.ConsultationTranscript;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

class TranscriptStructureJobWorkerTests {

    private final ProcessingJobRepository processingJobRepository = mock(ProcessingJobRepository.class);
    private final ConsultationTranscriptRepository transcriptRepository =
            mock(ConsultationTranscriptRepository.class);
    private final DocumentVersionRepository documentVersionRepository = mock(DocumentVersionRepository.class);
    private final ClinicalDocumentRepository clinicalDocumentRepository = mock(ClinicalDocumentRepository.class);
    private final MedicationDraftCandidateRepository medicationDraftCandidateRepository =
            mock(MedicationDraftCandidateRepository.class);
    private final TranscriptStructureJobWorker worker = new TranscriptStructureJobWorker(
            processingJobRepository,
            transcriptRepository,
            documentVersionRepository,
            clinicalDocumentRepository,
            medicationDraftCandidateRepository,
            new ObjectMapper(),
            mock(TransactionTemplate.class),
            mock(ClinicalNoteGenerationProvider.class)
    );

    @Test
    void savesPendingMedicationCandidatesWithTheGeneratedDocumentVersion() {
        Tenant tenant = new Tenant("Clinică test", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.test",
                "hash",
                "Medic test",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Pacient", "Test", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        ProcessingJob job = new ProcessingJob(
                tenant,
                consultation,
                ProcessingJobType.STRUCTURE_TRANSCRIPTION
        );
        ConsultationTranscript transcript = new ConsultationTranscript(
                tenant,
                consultation,
                "test",
                "test-model",
                "ro",
                "Se recomandă Paracetamol 500 mg de două ori pe zi.",
                "{}"
        );
        ClinicalDocument document = new ClinicalDocument(tenant, consultation, "SOAP_NOTE");
        UUID jobId = UUID.randomUUID();

        when(processingJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(clinicalDocumentRepository.findByConsultation_IdAndTenant_IdAndDocumentType(
                nullable(UUID.class),
                nullable(UUID.class),
                eq("SOAP_NOTE")
        )).thenReturn(Optional.of(document));
        when(documentVersionRepository.save(any(DocumentVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transcriptRepository.findByConsultation_IdAndTenant_Id(
                nullable(UUID.class),
                nullable(UUID.class)
        )).thenReturn(Optional.of(transcript));

        ClinicalNoteGenerationResult result = new ClinicalNoteGenerationResult(
                "test-provider",
                "test-model",
                "prompt-v2",
                "template-v1",
                new ClinicalNoteGenerationResult.SoapNote(
                        "Subiectiv",
                        "Obiectiv",
                        "Evaluare",
                        "Plan"
                ),
                List.of(new ClinicalNoteGenerationResult.MedicationCandidate(
                        MedicationMentionType.START,
                        "Paracetamol",
                        "500 mg",
                        null,
                        "de două ori pe zi",
                        null,
                        null,
                        null,
                        List.of()
                )),
                List.of("MEDICATION_REVIEW"),
                "{}"
        );

        worker.markSucceeded(jobId, result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MedicationDraftCandidate>> candidatesCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(medicationDraftCandidateRepository).saveAll(candidatesCaptor.capture());
        List<MedicationDraftCandidate> savedCandidates = new ArrayList<>();
        candidatesCaptor.getValue().forEach(savedCandidates::add);

        assertThat(savedCandidates).hasSize(1);
        MedicationDraftCandidate candidate = savedCandidates.getFirst();
        assertThat(candidate.getTenant()).isSameAs(tenant);
        assertThat(candidate.getConsultation()).isSameAs(consultation);
        assertThat(candidate.getTranscript()).isSameAs(transcript);
        assertThat(candidate.getDocumentVersion()).isNotNull();
        assertThat(candidate.getPosition()).isZero();
        assertThat(candidate.getMentionType()).isEqualTo(MedicationMentionType.START);
        assertThat(candidate.getReviewStatus()).isEqualTo(MedicationReviewStatus.PENDING_REVIEW);
        assertThat(candidate.getExtractedName()).isEqualTo("Paracetamol");
        assertThat(candidate.getCatalogMedication()).isNull();
        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.SUCCEEDED);
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.NOTES_READY);
    }
}
