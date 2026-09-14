package ro.scribemed.backend.knowledge.infrastructure.embedding.dto;

import java.util.List;

public record EmbeddingRequest(
        List<String> texts
) {
}
