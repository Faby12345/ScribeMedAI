package ro.scribemed.backend.processing.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.knowledge.application.*;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.dto.ExtractedPage;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;

import java.io.IOException;
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
public class IngestDocumentJobWorker {
    private static final Logger log = LoggerFactory.getLogger(IngestDocumentJobWorker.class);
    private final ProcessingJobRepository processingJobRepository;
    private final ChunkingService chunkingService;
    private final KnowledgeService knowledgeService;
    private final PdfService pdfService;
    private final EmbeddingProvider embeddingProvider;


    private final String workerId = "api-worker-" + UUID.randomUUID();



    public IngestDocumentJobWorker(ProcessingJobRepository processingJobRepository, ChunkingService chunkingService, KnowledgeService knowledgeService, PdfService pdfService, EmbeddingProvider embeddingProvider) {
        this.processingJobRepository = processingJobRepository;
        this.chunkingService = chunkingService;
        this.knowledgeService = knowledgeService;

        this.pdfService = pdfService;
        this.embeddingProvider = embeddingProvider;
    }

    @Scheduled(fixedDelayString = "${scribemed.processing.worker.poll-delay-ms}")
    public void processNextJob() throws IOException {
        Optional<ProcessingJob> claimedJob = claimNextJob();

        if (claimedJob == null || claimedJob.isEmpty()) {
            return;
        }


        ProcessingJob job = claimedJob.get();
        KnowledgeDocument knowledgeDocument = job.getKnowledgeDocument();

        byte[] pdfBytes = pdfService.read(knowledgeDocument.getObjectKey());

        List<ExtractedPage> extractedPages = PdfService.extract(pdfBytes);

        List<KnowledgeChunkDraft> chunkDrafts = chunkingService.chunk(extractedPages);


    }

    Optional<ProcessingJob> claimNextJob() {
        Optional<ProcessingJob> job = processingJobRepository.findNextIngestDocumentForUpdate();

        if (job.isEmpty()) {
            return Optional.empty();
        }

        ProcessingJob processingJob = job.get();

        processingJob.markRunning(workerId, Instant.now());

        log.info(
                "ingest_document_job_started tenantId={} obId={}",
                processingJob.getTenant().getId(),
                processingJob.getId()
        );

        return  Optional.of(processingJob);

    }

}
