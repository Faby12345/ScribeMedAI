package ro.scribemed.backend.document.dto;

import java.time.Instant;
import java.util.UUID;

import ro.scribemed.backend.document.domain.ClinicalDocumentStatus;
import ro.scribemed.backend.document.domain.DocumentVersionStatus;

public record DocumentApprovalResponse(
        UUID documentId,
        ClinicalDocumentStatus documentStatus,
        UUID versionId,
        int versionNumber,
        DocumentVersionStatus versionStatus,
        Instant approvedAt
) {
}
