package ro.scribemed.backend.knowledge.application;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.dto.KnowledgeDocumentRequest;
import ro.scribemed.backend.knowledge.dto.StoredPdf;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobType;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;

import java.io.IOException;
import java.io.InputStream;

@Service
public class KnowledgeDocumentIngestionService {
    private final TenantRepository tenantRepository;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkingService chunkingService;
    private final KnowledgeService knowledgeService;
    private final ProcessingJobRepository processingJobRepository;
    private final AuditService auditService;
    private final PdfService pdfService;



    public KnowledgeDocumentIngestionService(TenantRepository tenantRepository, EmbeddingProvider embeddingProvider, ChunkingService chunkingService, KnowledgeService knowledgeService, ProcessingJobRepository processingJobRepository, AuditService auditService, PdfService pdfService) {
        this.tenantRepository = tenantRepository;
        this.embeddingProvider = embeddingProvider;
        this.chunkingService = chunkingService;
        this.knowledgeService = knowledgeService;
        this.processingJobRepository = processingJobRepository;
        this.auditService = auditService;
        this.pdfService = pdfService;
    }

    @Transactional
    public void process(
            CurrentUser appUser,
            InputStream inputStream,
            long sizeByets,
            String originalFileName,
            KnowledgeDocumentRequest dto) throws IOException
    {
        Tenant tenant = tenantRepository.findById(appUser.tenantId())
                        .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));


        PdfValidator.validate(inputStream, sizeByets);


        StoredPdf storedPdf = pdfService.store(appUser.tenantId(), inputStream, sizeByets);

        KnowledgeDocument knowledgeDocument = knowledgeService.createDocument(
                new CreateKnowledgeDocumentCommand(
                        dto.title(),
                        dto.sourceInstitution(),
                        dto.sourceUrl(),
                        dto.publishedAt(),
                        dto.version(),
                        originalFileName,
                        storedPdf.checkSumSha256(),
                        storedPdf.objectKey()
                )
        );


        processingJobRepository.save(
                new ProcessingJob(
                        tenant,
                        knowledgeDocument,
                        ProcessingJobType.INGEST_DOCUMENT
                )
        );







    }


}
