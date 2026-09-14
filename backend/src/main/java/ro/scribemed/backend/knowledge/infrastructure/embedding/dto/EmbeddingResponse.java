package ro.scribemed.backend.knowledge.infrastructure.embedding.dto;

import java.util.List;

public record EmbeddingResponse(
        String model,
        int dimensions,
        List<List<Double>> embeddings
) {
}
