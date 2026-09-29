package ro.scribemed.backend.knowledge.application;

import org.junit.jupiter.api.Test;
import ro.scribemed.backend.knowledge.application.chunk.ChunkingService;
import ro.scribemed.backend.knowledge.application.chunk.KnowledgeChunkDraft;
import ro.scribemed.backend.knowledge.dto.ExtractedPage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingServiceTest {

    @Test
    void createsOneChunkForShortContent() {
        ChunkingService service = new ChunkingService(20, 4);

        List<KnowledgeChunkDraft> chunks = service.chunk(List.of(
                new ExtractedPage(1, "Primul paragraf.\n\nAl doilea paragraf.")
        ));

        assertEquals(1, chunks.size());
        assertEquals(0, chunks.getFirst().chunkIndex());
        assertEquals(1, chunks.getFirst().pageFrom());
        assertEquals(1, chunks.getFirst().pageTo());
        assertEquals(
                "Primul paragraf.\n\nAl doilea paragraf.",
                chunks.getFirst().content()
        );
    }

    @Test
    void createsOverlappingChunksWithoutExceedingMaximumSize() {
        ChunkingService service = new ChunkingService(8, 2);

        List<KnowledgeChunkDraft> chunks = service.chunk(List.of(
                new ExtractedPage(
                        1,
                        "Unu doi trei patru. Cinci șase șapte opt. "
                                + "Nouă zece unsprezece doisprezece."
                )
        ));

        assertEquals(2, chunks.size());
        assertEquals(
                "Unu doi trei patru. Cinci șase șapte opt.",
                chunks.get(0).content()
        );
        assertEquals(
                "șapte opt. Nouă zece unsprezece doisprezece.",
                chunks.get(1).content()
        );
        assertTrue(wordCount(chunks.get(1).content()) <= 8);
    }

    @Test
    void keepsThePageRangeWhenAChunkCrossesPages() {
        ChunkingService service = new ChunkingService(20, 4);

        List<KnowledgeChunkDraft> chunks = service.chunk(List.of(
                new ExtractedPage(2, "Textul de pe pagina a doua."),
                new ExtractedPage(1, "Textul de pe prima pagină.")
        ));

        assertEquals(1, chunks.size());
        assertEquals(1, chunks.getFirst().pageFrom());
        assertEquals(2, chunks.getFirst().pageTo());
        assertEquals(
                "Textul de pe prima pagină.\n\n"
                        + "Textul de pe pagina a doua.",
                chunks.getFirst().content()
        );
    }

    @Test
    void splitsAnOversizedSentence() {
        ChunkingService service = new ChunkingService(5, 1);

        List<KnowledgeChunkDraft> chunks = service.chunk(List.of(
                new ExtractedPage(3, "unu doi trei patru cinci șase șapte")
        ));

        assertEquals(2, chunks.size());
        assertEquals("unu doi trei patru cinci", chunks.get(0).content());
        assertEquals("cinci șase șapte", chunks.get(1).content());
        assertEquals(3, chunks.get(1).pageFrom());
        assertEquals(3, chunks.get(1).pageTo());
    }

    @Test
    void ignoresBlankAndNullPageEntries() {
        ChunkingService service = new ChunkingService(20, 4);

        List<KnowledgeChunkDraft> chunks = service.chunk(
                java.util.Arrays.asList(
                        null,
                        new ExtractedPage(1, "   "),
                        new ExtractedPage(2, "Conținut valid.")
                )
        );

        assertEquals(1, chunks.size());
        assertEquals(2, chunks.getFirst().pageFrom());
    }

    @Test
    void validatesChunkConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ChunkingService(0, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ChunkingService(10, 10)
        );
    }

    private int wordCount(String text) {
        return text.split("\\s+").length;
    }
}
