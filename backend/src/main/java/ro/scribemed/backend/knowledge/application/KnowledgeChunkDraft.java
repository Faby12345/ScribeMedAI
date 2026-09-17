package ro.scribemed.backend.knowledge.application;

public record KnowledgeChunkDraft(
        int chunkIndex,
        String content,
        int pageFrom,
        int pageTo
) {
}
