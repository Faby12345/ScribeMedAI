package ro.scribemed.backend.processing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationProvider;
import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationProviderException;
import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationResult;
import ro.scribemed.backend.consultation.dto.NotesRequest;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.domain.DocumentVersionSource;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.document.infrastructure.DocumentVersionRepository;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@ConditionalOnProperty(
        prefix = "scribemed.processing.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ProcessNoteJobWorker {
    private static final Logger log = LoggerFactory.getLogger(ProcessNoteJobWorker.class);

    private final ProcessingJobRepository processingJobRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ClinicalDocumentRepository clinicalDocumentRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ClinicalNoteGenerationProvider clinicalNoteGenerationProvider;

    private final String workerId = "api-worker-" + UUID.randomUUID();


    public ProcessNoteJobWorker(ProcessingJobRepository processingJobRepository,
                                DocumentVersionRepository documentVersionRepository,
                                ClinicalDocumentRepository clinicalDocumentRepository, ObjectMapper objectMapper, TransactionTemplate transactionTemplate, ClinicalNoteGenerationProvider clinicalNoteGenerationProvider) {
        this.processingJobRepository = processingJobRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.clinicalNoteGenerationProvider = clinicalNoteGenerationProvider;
    }

    @Scheduled(fixedDelayString = "${scribemed.processing.worker.poll-delay-ms}")
    public void processNextNoteJob() {
        Optional<ClaimedJob> claimedJob = transactionTemplate.execute(status -> claimNextJob());

        if (claimedJob == null || claimedJob.isEmpty()) {
            return;
        }

        ProcessingJob job = claimedJob.get().job();
        ConsultationNotes notes = claimedJob.get().notes();

        try {
            ClinicalNoteGenerationResult result = clinicalNoteGenerationProvider.generateFromDoctorNotes(new NotesRequest(
                    notes.getReason(),
                    notes.getAssessment(),
                    notes.getHistory(),
                    notes.getObjective(),
                    notes.getPlan()
            ));

            transactionTemplate.executeWithoutResult(status ->
                    markSucceeded(job.getId(), result)
            );
        } catch (ClinicalNoteGenerationProviderException error) {
            transactionTemplate.executeWithoutResult(status ->
                    markFailed(job.getId(), error.getSafeErrorCode(), error.getMessage())
            );
        } catch (RuntimeException error) {
            transactionTemplate.executeWithoutResult(status ->
                    markFailed(
                            job.getId(),
                            "NOTE_GENERATION_UNEXPECTED_ERROR",
                            "Clinical note generation failed unexpectedly"
                    )
            );
        }
    }

    Optional<ClaimedJob> claimNextJob() {
        Optional<ProcessingJob> job = processingJobRepository.findNextNoteJobForUpdate();

        if (job.isEmpty()) {
            return Optional.empty();
        }

        ProcessingJob processingJob = job.get();
        Consultation consultation = processingJob.getConsultation();
        ConsultationNotes notes = processingJob.getSourceNotes();

        if (notes == null) {
            throw new IllegalStateException("Note processing job has no source notes");
        }

        if (!notes.getTenant().getId().equals(processingJob.getTenant().getId())
                || !notes.getConsultation().getId().equals(consultation.getId())) {
            throw new IllegalStateException("Note processing job source notes do not match job tenant or consultation");
        }

        processingJob.markRunning(workerId, Instant.now());
        consultation.markProcessingNotes();

        log.info(
                "note_processing_job_started tenantId={} consultationId={} jobId={}",
                processingJob.getTenant().getId(),
                consultation.getId(),
                processingJob.getId()
        );

        return Optional.of(new ClaimedJob(processingJob, notes));
    }

    void markSucceeded(UUID jobId, ClinicalNoteGenerationResult result) {
        ProcessingJob job = processingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Processing job not found"));

        ConsultationNotes notes = job.getSourceNotes();
        if (notes == null) {
            throw new IllegalStateException("Processing job has no source notes");
        }

        if (!documentVersionRepository.existsBySourceNotes_IdAndTenant_Id(
                notes.getId(),
                job.getTenant().getId()
        )) {
            ClinicalDocument document = clinicalDocumentRepository
                    .findByConsultation_IdAndTenant_IdAndDocumentType(
                            job.getConsultation().getId(),
                            job.getTenant().getId(),
                            "SOAP_NOTE"
                    )
                    .orElseGet(() -> clinicalDocumentRepository.save(new ClinicalDocument(
                            job.getTenant(),
                            job.getConsultation(),
                            "SOAP_NOTE"
                    )));

            int versionNumber = document.nextVersionNumber();

            documentVersionRepository.save(new DocumentVersion(
                    job.getTenant(),
                    document,
                    job.getConsultation(),
                    notes,
                    versionNumber,
                    DocumentVersionSource.AI_GENERATED,
                    result.soapNote().subjective(),
                    result.soapNote().objective(),
                    result.soapNote().assessment(),
                    result.soapNote().plan(),
                    writeJson(result.reviewFlags()),
                    result.provider(),
                    result.model(),
                    result.promptVersion(),
                    result.templateVersion(),
                    notes.getCreatedByUser()
            ));
        }

        job.getConsultation().markNotesReady();
        job.markSucceeded(Instant.now());

        log.info(
                "note_processing_job_succeeded tenantId={} consultationId={} jobId={} provider={}",
                job.getTenant().getId(),
                job.getConsultation().getId(),
                job.getId(),
                result.provider()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Review flags could not be serialized", error);
        }
    }






    void markFailed(UUID jobId, String errorCode, String safeMessage) {
        boolean willRetry;
        ProcessingJob job = processingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Processing job not found"));

        if (job.canRetry()) {
            willRetry = true;
            job.markRetry(errorCode, safeMessage, Instant.now().plus(Duration.ofSeconds(30)));
        } else {
            willRetry = false;
            job.markFailed(errorCode, safeMessage, Instant.now());
            job.getConsultation().markNotesFailed();
        }

        log.info(
                "note_processing_job_failed tenantId={} consultationId={} jobId={} errorCode={} willRetry={}",
                job.getTenant().getId(),
                job.getConsultation().getId(),
                job.getId(),
                errorCode,
                willRetry
        );
    }

    private record ClaimedJob(
            ProcessingJob job,
            ConsultationNotes notes
    ) {
    }



}
