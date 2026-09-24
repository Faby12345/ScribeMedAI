package ro.scribemed.backend.knowledge.application;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.knowledge.application.chunk.ChunkingService;
import ro.scribemed.backend.knowledge.application.pdf.PdfService;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.dto.KnowledgeDocumentRequest;
import ro.scribemed.backend.knowledge.dto.StoredPdf;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDocumentIngestionServiceTest {

    @Test
    void storesTheCompletePdfAfterValidation() throws IOException {
        byte[] pdf = createPdf();
        UUID tenantId = UUID.randomUUID();
        TenantRepository tenants = mock(TenantRepository.class);
        PdfService pdfService = mock(PdfService.class);
        KnowledgeService knowledgeService = mock(KnowledgeService.class);
        ProcessingJobRepository jobs = mock(ProcessingJobRepository.class);
        Tenant tenant = new Tenant("Clinică test", TenantStatus.ACTIVE);
        when(tenants.findById(tenantId))
                .thenReturn(Optional.of(tenant));
        when(pdfService.store(eq(tenantId), any(), eq((long) pdf.length)))
                .thenAnswer(invocation -> {
                    assertArrayEquals(pdf, invocation.<java.io.InputStream>getArgument(1).readAllBytes());
                    return new StoredPdf("tenant/test/knowledge/file.pdf", "a".repeat(64), pdf.length);
                });
        when(knowledgeService.createDocument(any(), any()))
                .thenReturn(mock(KnowledgeDocument.class));

        KnowledgeDocumentIngestionService service = new KnowledgeDocumentIngestionService(
                tenants, mock(EmbeddingProvider.class), mock(ChunkingService.class),
                knowledgeService, jobs, mock(AuditService.class), pdfService);
        service.process(
                new CurrentUser(UUID.randomUUID(), tenantId, null, null, null, null),
                new ByteArrayInputStream(pdf), pdf.length, "ghid.pdf",
                new KnowledgeDocumentRequest("Ghid", "Instituție", null, null, null)
        );

        verify(pdfService).store(eq(tenantId), any(), eq((long) pdf.length));
        verify(knowledgeService).createDocument(eq(tenant), any());
        verify(jobs).save(any());
    }

    private byte[] createPdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
