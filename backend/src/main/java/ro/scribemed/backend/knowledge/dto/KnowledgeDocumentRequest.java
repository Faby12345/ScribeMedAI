package ro.scribemed.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record KnowledgeDocumentRequest(
        @NotBlank
        @Size(max = 500)
        String title,

        @NotBlank
        @Size(max = 255)
        String sourceInstitution,
        String sourceUrl,

        @PastOrPresent
        LocalDate publishedAt,
        String version
) {
}