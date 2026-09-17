package ro.scribemed.backend.knowledge.application;

import java.util.List;
import java.util.Objects;

public record EmbeddedKnowledgeChunkDraft(
        KnowledgeChunkDraft chunk,
        String sectionTitle,
        List<Double> embedding
) {
    public EmbeddedKnowledgeChunkDraft {
        Objects.requireNonNull(chunk, "chunk must not be null");
        Objects.requireNonNull(embedding, "embedding must not be null");
        embedding = List.copyOf(embedding);
    }
}
