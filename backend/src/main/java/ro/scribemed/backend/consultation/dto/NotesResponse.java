package ro.scribemed.backend.consultation.dto;

import ro.scribemed.backend.document.domain.DocumentVersionStatus;

import java.util.UUID;

public record NotesResponse(
        UUID notesId,
        UUID documentId,
        UUID versionId,
        UUID consultationId,
        DocumentVersionStatus status
) {
}
