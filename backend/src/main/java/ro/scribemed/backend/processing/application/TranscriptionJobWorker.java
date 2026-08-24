package ro.scribemed.backend.processing.application;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ro.scribemed.backend.audio.application.AudioStorageService;
import ro.scribemed.backend.audio.domain.ConsultationAudio;
import ro.scribemed.backend.audio.infrastructure.ConsultationAudioRepository;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobStatus;
import ro.scribemed.backend.processing.domain.ProcessingJobType;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;
import ro.scribemed.backend.transcription.application.TranscriptionProvider;
import ro.scribemed.backend.transcription.application.TranscriptionProviderException;
import ro.scribemed.backend.transcription.dto.TranscriptionRequest;
import ro.scribemed.backend.transcription.application.TranscriptionResult;
import ro.scribemed.backend.transcription.domain.ConsultationTranscript;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

@Service
@ConditionalOnProperty(
        prefix = "scribemed.processing.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TranscriptionJobWorker {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionJobWorker.class);

    private final ProcessingJobRepository processingJobRepository;
    private final ConsultationAudioRepository audioRepository;
    private final ConsultationTranscriptRepository transcriptRepository;
    private final AudioStorageService audioStorageService;
    private final TranscriptionProvider transcriptionProvider;
    private final TransactionTemplate transactionTemplate;
    private final String workerId = "api-worker-" + UUID.randomUUID();

    public TranscriptionJobWorker(
            ProcessingJobRepository processingJobRepository,
            ConsultationAudioRepository audioRepository,
            ConsultationTranscriptRepository transcriptRepository,
            AudioStorageService audioStorageService,
            TranscriptionProvider transcriptionProvider,
            TransactionTemplate transactionTemplate
    ) {
        this.processingJobRepository = processingJobRepository;
        this.audioRepository = audioRepository;
        this.transcriptRepository = transcriptRepository;
        this.audioStorageService = audioStorageService;
        this.transcriptionProvider = transcriptionProvider;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${scribemed.processing.worker.poll-delay-ms}")
    public void processNextTranscriptionJob() {
        Optional<ClaimedJob> claimedJob = transactionTemplate.execute(status -> claimNextJob());

        if (claimedJob.isEmpty()) {
            return;
        }

        ProcessingJob job = claimedJob.get().job();
        ConsultationAudio audio = claimedJob.get().audio();

        try {
            byte[] audioData = audioStorageService.read(audio.getObjectKey());
            TranscriptionResult result = transcriptionProvider.transcribe(new TranscriptionRequest(
                    audioData,
                    audio.getContentType()
            ));
            transactionTemplate.executeWithoutResult(status -> markSucceeded(job.getId(), result));
        } catch (TranscriptionProviderException error) {
            transactionTemplate.executeWithoutResult(status ->
                    markFailed(job.getId(), error.getSafeErrorCode(), error.getMessage())
            );
        } catch (IOException error) {
            transactionTemplate.executeWithoutResult(status ->
                    markFailed(job.getId(), "AUDIO_READ_FAILED", "Audio file could not be read")
            );
        } catch (RuntimeException error) {
            transactionTemplate.executeWithoutResult(status ->
                    markFailed(job.getId(), "TRANSCRIPTION_UNEXPECTED_ERROR", "Transcription failed unexpectedly")
            );
        }
    }

    Optional<ClaimedJob> claimNextJob() {
        Optional<ProcessingJob> job = processingJobRepository.findNextTranscriptionJobForUpdate();

        if (job.isEmpty()) {
            return Optional.empty();
        }

        ProcessingJob processingJob = job.get();
        Consultation consultation = processingJob.getConsultation();
        ConsultationAudio audio = audioRepository
                .findFirstByConsultation_IdAndTenant_IdOrderByCreatedAtDesc(
                        consultation.getId(),
                        processingJob.getTenant().getId()
                )
                .orElseThrow(() -> new IllegalStateException("Consultation audio not found"));

        processingJob.markRunning(workerId, Instant.now());
        consultation.markTranscribing();

        log.info(
                "transcription_job_started tenantId={} consultationId={} jobId={} provider=deepgram",
                processingJob.getTenant().getId(),
                consultation.getId(),
                processingJob.getId()
        );

        return Optional.of(new ClaimedJob(processingJob, audio));
    }

    void markSucceeded(UUID jobId, TranscriptionResult result) {
        ProcessingJob job = processingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Processing job not found"));

        if (!transcriptRepository.existsByConsultation_Id(job.getConsultation().getId())) {
            transcriptRepository.save(new ConsultationTranscript(
                    job.getTenant(),
                    job.getConsultation(),
                    result.provider(),
                    result.model(),
                    result.language(),
                    result.transcriptText(),
                    result.rawProviderResponse()
            ));
        }

        audioRepository.findFirstByConsultation_IdAndTenant_IdOrderByCreatedAtDesc(
                job.getConsultation().getId(),
                job.getTenant().getId()
        ).ifPresent(ConsultationAudio::markTranscribed);

        job.getConsultation().markTranscriptionReady();
        enqueueTranscriptStructureJobIfNeeded(job);
        job.markSucceeded(Instant.now());

        log.info(
                "transcription_job_succeeded tenantId={} consultationId={} jobId={} provider={}",
                job.getTenant().getId(),
                job.getConsultation().getId(),
                job.getId(),
                result.provider()
        );
    }

    private void enqueueTranscriptStructureJobIfNeeded(ProcessingJob job) {
        boolean activeJobExists = processingJobRepository.existsByConsultation_IdAndTenant_IdAndJobTypeAndStatusIn(
                job.getConsultation().getId(),
                job.getTenant().getId(),
                ProcessingJobType.STRUCTURE_TRANSCRIPTION,
                List.of(
                        ProcessingJobStatus.PENDING,
                        ProcessingJobStatus.RUNNING,
                        ProcessingJobStatus.RETRY
                )
        );

        if (!activeJobExists) {
            processingJobRepository.save(new ProcessingJob(
                    job.getTenant(),
                    job.getConsultation(),
                    ProcessingJobType.STRUCTURE_TRANSCRIPTION
            ));
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
            job.getConsultation().markTranscriptionFailed();
            audioRepository.findFirstByConsultation_IdAndTenant_IdOrderByCreatedAtDesc(
                    job.getConsultation().getId(),
                    job.getTenant().getId()
            ).ifPresent(ConsultationAudio::markFailed);
        }

        log.info(
                "transcription_job_failed tenantId={} consultationId={} jobId={} errorCode={} willRetry={}",
                job.getTenant().getId(),
                job.getConsultation().getId(),
                job.getId(),
                errorCode,
                willRetry
        );
    }

    private record ClaimedJob(ProcessingJob job, ConsultationAudio audio) {
    }
}
