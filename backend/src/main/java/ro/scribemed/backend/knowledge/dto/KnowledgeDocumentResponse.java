package ro.scribemed.backend.knowledge.dto;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentResponse(
        UUID id,
        String fileName,
        Instant createdAt
        ) { }
