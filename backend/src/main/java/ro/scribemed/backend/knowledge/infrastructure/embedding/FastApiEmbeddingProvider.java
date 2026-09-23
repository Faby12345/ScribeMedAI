package ro.scribemed.backend.knowledge.infrastructure.embedding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.scribemed.backend.knowledge.application.EmbeddingProvider;
import ro.scribemed.backend.knowledge.infrastructure.embedding.dto.EmbeddingRequest;
import ro.scribemed.backend.knowledge.infrastructure.embedding.dto.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class FastApiEmbeddingProvider implements EmbeddingProvider {
    private static final int BATCH_SIZE = 32;

    private final RestClient restClient;
    private final int expectedDimensions;
    public FastApiEmbeddingProvider(
            @Qualifier("embeddingRestClient") RestClient restClient,
            @Value("${scribemed.embedding.expected-dimensions}")
            int expectedDimensions
    ) {
        this.restClient = restClient;
        this.expectedDimensions = expectedDimensions;
    }
    @Override
    public List<Double> embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<List<Double>> embedAll(List<String> texts) {
        Objects.requireNonNull(texts, "texts must not be null");
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("Embedding texts must not be blank");
        }
        List<List<Double>> embeddings = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            List<String> batch = texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()));
            embeddings.addAll(embedBatch(batch));
        }
        return List.copyOf(embeddings);
    }

    private List<List<Double>> embedBatch(List<String> texts) {
        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new EmbeddingRequest(texts))
                .retrieve()
                .body(EmbeddingResponse.class);
        if (response == null || response.embeddings() == null
                || response.embeddings().size() != texts.size()) {
            throw new IllegalStateException("Invalid embedding service response");
        }

        for (List<Double> embedding : response.embeddings()) {
            if (embedding == null || embedding.size() != expectedDimensions
                    || embedding.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
                throw new IllegalStateException("Invalid embedding service response");
            }
        }
        return response.embeddings();
    }
}
