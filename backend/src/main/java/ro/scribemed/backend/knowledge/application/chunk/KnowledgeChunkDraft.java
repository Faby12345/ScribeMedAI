package ro.scribemed.backend.knowledge.application.chunk;

public record KnowledgeChunkDraft(
        int chunkIndex,
        String content,
        int pageFrom,
        int pageTo
) {
}
