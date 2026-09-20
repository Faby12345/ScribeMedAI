package ro.scribemed.backend.knowledge.application;

import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import ro.scribemed.backend.knowledge.domain.KnowledgeChunk;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocumentStatus;
import ro.scribemed.backend.knowledge.infrastructure.KnowledgeChunkRepository;
import ro.scribemed.backend.knowledge.infrastructure.KnowledgeDocumentRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeServiceTest {

    private KnowledgeDocumentRepository documentRepository;
    private KnowledgeChunkRepository chunkRepository;
    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(KnowledgeDocumentRepository.class);
        chunkRepository = mock(KnowledgeChunkRepository.class);
        service = new KnowledgeService(documentRepository, chunkRepository);
    }

    @Test
    void createsAProcessingDocumentWithNormalizedValues() {
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocument document = service.createDocument(
                new CreateKnowledgeDocumentCommand(
                        "  Ghid clinic  ",
                        "  Ministerul Sănătății  ",
                        "  https://example.test/ghid.pdf  ",
                        LocalDate.of(2026, 1, 10),
                        "  2.0  ",
                        "  ghid.pdf  ",
                        "A".repeat(64),
                        "  tenant/test/knowledge/ghid.pdf  "
                )
        );

        assertEquals("Ghid clinic", document.getTitle());
        assertEquals("Ministerul Sănătății", document.getSourceInstitution());
        assertEquals("a".repeat(64), document.getChecksum());
        assertEquals("tenant/test/knowledge/ghid.pdf", document.getObjectKey());
        assertEquals(KnowledgeDocumentStatus.PROCESSING, document.getStatus());
    }

    @Test
    void rejectsADuplicateDocumentChecksum() {
        String checksum = "b".repeat(64);
        when(documentRepository.existsByChecksum(checksum)).thenReturn(true);

        assertThrows(
                EntityExistsException.class,
                () -> service.createDocument(command(checksum))
        );

        verify(documentRepository, never()).save(any());
    }

    @Test
    void replacesChunksAndActivatesTheDocument() {
        UUID documentId = UUID.randomUUID();
        KnowledgeDocument document = document();
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(chunkRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<EmbeddedKnowledgeChunkDraft> drafts = List.of(
                embeddedChunk(1, "Al doilea fragment", 2),
                embeddedChunk(0, "Primul fragment", 1)
        );

        List<KnowledgeChunk> chunks = service.replaceChunksAndActivate(
                documentId,
                drafts
        );

        assertEquals(KnowledgeDocumentStatus.ACTIVE, document.getStatus());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(1, chunks.get(1).getChunkIndex());
        verify(chunkRepository).deleteAllByDocumentId(documentId);
    }

    @Test
    void rejectsAnEmbeddingWithTheWrongDimensions() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document()));

        EmbeddedKnowledgeChunkDraft invalid =
                new EmbeddedKnowledgeChunkDraft(
                        new KnowledgeChunkDraft(0, "Fragment", 1, 1),
                        null,
                        List.of(0.1, 0.2)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.replaceChunksAndActivate(
                        documentId,
                        List.of(invalid)
                )
        );

        verify(chunkRepository, never()).deleteAllByDocumentId(any());
    }

    @Test
    void retrievesOnlyActiveChunksWithTheRequestedLimit() {
        List<Double> embedding = embedding();

        service.findRelevantChunks(embedding, 8);

        ArgumentCaptor<float[]> embeddingCaptor =
                ArgumentCaptor.forClass(float[].class);
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(chunkRepository).findNearestByCosineDistance(
                embeddingCaptor.capture(),
                eq(KnowledgeDocumentStatus.ACTIVE),
                pageableCaptor.capture()
        );
        assertEquals(384, embeddingCaptor.getValue().length);
        assertEquals(8, pageableCaptor.getValue().getPageSize());
    }

    private CreateKnowledgeDocumentCommand command(String checksum) {
        return new CreateKnowledgeDocumentCommand(
                "Ghid clinic",
                "Ministerul Sănătății",
                null,
                null,
                null,
                "ghid.pdf",
                checksum,
                "tenant/test/knowledge/ghid.pdf"
        );
    }

    private KnowledgeDocument document() {
        return new KnowledgeDocument(
                "Ghid clinic",
                "Ministerul Sănătății",
                null,
                null,
                null,
                "ghid.pdf",
                "c".repeat(64),
                "tenant/test/knowledge/ghid.pdf"
        );
    }

    private EmbeddedKnowledgeChunkDraft embeddedChunk(
            int index,
            String content,
            int page
    ) {
        return new EmbeddedKnowledgeChunkDraft(
                new KnowledgeChunkDraft(index, content, page, page),
                null,
                embedding()
        );
    }

    private List<Double> embedding() {
        return new ArrayList<>(Collections.nCopies(384, 0.1));
    }
}
