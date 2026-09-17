package ro.scribemed.backend.knowledge.application;

import java.time.LocalDate;

public record CreateKnowledgeDocumentCommand(
        String title,
        String sourceInstitution,
        String sourceUrl,
        LocalDate publishedAt,
        String version,
        String originalFilename,
        String checksum
) {
}
