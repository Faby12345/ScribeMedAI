package ro.scribemed.backend.processing.application;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ro.scribemed.backend.knowledge.application.ChunkingService;
import ro.scribemed.backend.knowledge.application.EmbeddedKnowledgeChunkDraft;
import ro.scribemed.backend.knowledge.application.EmbeddingProvider;
import ro.scribemed.backend.knowledge.application.KnowledgeChunkDraft;
import ro.scribemed.backend.knowledge.application.KnowledgeService;
import ro.scribemed.backend.knowledge.application.PdfService;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.dto.ExtractedPage;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@ConditionalOnProperty(
        prefix = "scribemed.processing.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class IngestDocumentJobWorker {
    private static final Logger log = LoggerFactory.getLogger(IngestDocumentJobWorker.class);
    private final ProcessingJobRepository processingJobRepository;
    private final ChunkingService chunkingService;
    private final KnowledgeService knowledgeService;
    private final PdfService pdfService;
    private final EmbeddingProvider embeddingProvider;
    private final TransactionTemplate transactionTemplate;
    private final String workerId = "api-worker-" + UUID.randomUUID();

    public IngestDocumentJobWorker(
            ProcessingJobRepository processingJobRepository,
            ChunkingService chunkingService,
            KnowledgeService knowledgeService,
            PdfService pdfService,
            EmbeddingProvider embeddingProvider,
            TransactionTemplate transactionTemplate
    ) {
        this.processingJobRepository = processingJobRepository;
        this.chunkingService = chunkingService;
        this.knowledgeService = knowledgeService;
        this.pdfService = pdfService;
        this.embeddingProvider = embeddingProvider;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${scribemed.processing.worker.poll-delay-ms}")
    public void processNextJob() {
        Optional<ClaimedJob> claimedJob = transactionTemplate.execute(status -> claimNextJob());

        if (claimedJob == null || claimedJob.isEmpty()) {
            return;
        }

        ClaimedJob job = claimedJob.get();
        try {
            byte[] pdfBytes;
            try {
                pdfBytes = pdfService.read(job.objectKey());
            } catch (IOException error) {
                transactionTemplate.executeWithoutResult(status -> markFailed(job.jobId(), "PDF_READ_FAILED"));
                return;
            }
            List<ExtractedPage> extractedPages;
            try {
                extractedPages = PdfService.extract(pdfBytes);
            } catch (IOException error) {
                transactionTemplate.executeWithoutResult(status -> markFailed(job.jobId(), "PDF_EXTRACT_FAILED"));
                return;
            }
            List<KnowledgeChunkDraft> chunkDrafts = chunkingService.chunk(extractedPages);
            List<List<Double>> embeddings = embeddingProvider.embedAll(
                    chunkDrafts.stream().map(chunk -> "passage: " + chunk.content()).toList()
            );
            if (embeddings.size() != chunkDrafts.size()) {
                throw new IllegalStateException("Embedding count does not match chunk count");
            }
            List<EmbeddedKnowledgeChunkDraft> embeddedDrafts =
                    IntStream.range(0, chunkDrafts.size())
                            .mapToObj(index -> new EmbeddedKnowledgeChunkDraft(
                                    chunkDrafts.get(index), null, embeddings.get(index)))
                            .toList();
            transactionTemplate.executeWithoutResult(status -> {
                knowledgeService.replaceChunksAndActivate(job.documentId(), embeddedDrafts);
                processingJobRepository.findById(job.jobId())
                        .orElseThrow(() -> new IllegalStateException("Processing job not found"))
                        .markSucceeded(Instant.now());
            });
            ProcessingJob processingJob = processingJobRepository.findById(job.jobId())
                    .orElseThrow(() -> new EntityNotFoundException("Processing job not found"));
            log.info(
                    "ingest_document_job_finished tenantId={} jobId={}",
                    processingJob.getTenant().getId(),
                    processingJob.getId()
            );

        } catch (RuntimeException error) {
            transactionTemplate.executeWithoutResult(status -> markFailed(job.jobId(), "DOCUMENT_INGEST_FAILED"));
        }
    }

    Optional<ClaimedJob> claimNextJob() {
        Optional<ProcessingJob> job = processingJobRepository.findNextIngestDocumentForUpdate();

        if (job.isEmpty()) {
            return Optional.empty();
        }

        ProcessingJob processingJob = job.get();

        processingJob.markRunning(workerId, Instant.now());

        log.info(
                "ingest_document_job_started tenantId={} jobId={}",
                processingJob.getTenant().getId(),
                processingJob.getId()
        );

        KnowledgeDocument document = processingJob.getKnowledgeDocument();
        return Optional.of(new ClaimedJob(processingJob.getId(), document.getId(), document.getObjectKey()));
    }

    private void markFailed(UUID jobId, String errorCode) {
        ProcessingJob job = processingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Processing job not found"));
        boolean willRetry = job.canRetry();
        if (willRetry) {
            job.markRetry(errorCode, "Document processing failed", Instant.now().plus(Duration.ofSeconds(30)));
        } else {
            job.markFailed(errorCode, "Document processing failed", Instant.now());
            knowledgeService.markDocumentFailed(job.getKnowledgeDocument().getId());
        }
        log.warn("ingest_document_job_failed tenantId={} jobId={} errorCode={} willRetry={}",
                job.getTenant().getId(), jobId, errorCode, willRetry);
    }

    private record ClaimedJob(UUID jobId, UUID documentId, String objectKey) {
    }
}
