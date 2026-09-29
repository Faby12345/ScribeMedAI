package ro.scribemed.backend.document.dto;

import java.time.Instant;
import java.util.UUID;

import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.ClinicalDocumentStatus;

public record PatientDocumentSummaryResponse(
        UUID documentId,
        UUID consultationId,
        String documentType,
        ClinicalDocumentStatus status,
        int currentVersionNumber,
        Instant consultationCreatedAt,
        Instant documentCreatedAt,
        Instant documentUpdatedAt,
        Instant approvedAt
) {

    public static PatientDocumentSummaryResponse from(ClinicalDocument document) {
        return new PatientDocumentSummaryResponse(
                document.getId(),
                document.getConsultation().getId(),
                document.getDocumentType(),
                document.getStatus(),
                document.getCurrentVersionNumber(),
                document.getConsultation().getCreatedAt(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getApprovedAt()
        );
    }
}
