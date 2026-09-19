package ro.scribemed.backend.processing.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.scribemed.backend.knowledge.application.ChunkingService;
import ro.scribemed.backend.knowledge.application.KnowledgeService;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;

public class IngestDocumentJobWorker {
    private static final Logger log = LoggerFactory.getLogger(IngestDocumentJobWorker.class);
    private final ProcessingJobRepository processingJobRepository;
    private final ChunkingService chunkingService;
    private final KnowledgeService knowledgeService;


    public IngestDocumentJobWorker(ProcessingJobRepository processingJobRepository, ChunkingService chunkingService, KnowledgeService knowledgeService) {
        this.processingJobRepository = processingJobRepository;
        this.chunkingService = chunkingService;
        this.knowledgeService = knowledgeService;
    }
}
