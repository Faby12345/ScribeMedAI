package ro.scribemed.backend.knowledge.dto;

import java.util.List;
import java.util.UUID;

public record KnowledgeQueryResponse(
        String answer,
        List<KnowledgeCitationResponse> citations
) {
    public record KnowledgeCitationResponse(
            UUID documentId,
            String documentTitle,
            Integer pageFrom,
            Integer pageTo,
            String sectionTitle,
            String excerpt
    ) {
    }
}
