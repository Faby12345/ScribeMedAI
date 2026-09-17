package ro.scribemed.backend.knowledge.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ro.scribemed.backend.knowledge.domain.KnowledgeChunk;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocumentStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18")
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("scribemed_knowledge_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private KnowledgeChunkRepository chunkRepository;

    @Test
    void persistsVectorsAndRetrievesTheNearestActiveChunk() {
        KnowledgeDocument document = documentRepository.saveAndFlush(
                new KnowledgeDocument(
                        "Ghid clinic",
                        "Ministerul Sănătății",
                        null,
                        null,
                        null,
                        "ghid.pdf",
                        "d".repeat(64)
                )
        );

        float[] closeEmbedding = embedding(1.0f, 0.0f);
        float[] distantEmbedding = embedding(0.0f, 1.0f);

        chunkRepository.saveAllAndFlush(List.of(
                new KnowledgeChunk(
                        document,
                        0,
                        "Fragment apropiat",
                        1,
                        1,
                        null,
                        closeEmbedding
                ),
                new KnowledgeChunk(
                        document,
                        1,
                        "Fragment îndepărtat",
                        2,
                        2,
                        null,
                        distantEmbedding
                )
        ));
        document.markActive();
        documentRepository.flush();

        List<KnowledgeChunk> result =
                chunkRepository.findNearestByCosineDistance(
                        closeEmbedding,
                        KnowledgeDocumentStatus.ACTIVE,
                        PageRequest.of(0, 1)
                );

        assertEquals(1, result.size());
        assertEquals("Fragment apropiat", result.getFirst().getContent());
    }

    private float[] embedding(float first, float second) {
        float[] embedding = new float[KnowledgeChunk.EMBEDDING_DIMENSIONS];
        embedding[0] = first;
        embedding[1] = second;
        return embedding;
    }
}
