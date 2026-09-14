package ro.scribemed.backend.knowledge.infrastructure.embedding;

import org.junit.jupiter.api.Test;
import ro.scribemed.backend.knowledge.application.EmbeddingProvider;
import ro.scribemed.backend.shared.http.Http11RestClientBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;



class FastApiEmbeddingProviderTest {

    private final EmbeddingProvider embeddingProvider =
            new FastApiEmbeddingProvider(
                    Http11RestClientBuilder.build()
                            .baseUrl("http://localhost:8000")
                            .build(),
                    384
            );

    @Test
    void shouldGenerate384DimensionalEmbedding() {

        List<Double> embedding = embeddingProvider.embed("query: Care este tratamentul schizofreniei?");
        assertNotNull(embedding);
        assertEquals(384, embedding.size());

    }

}
