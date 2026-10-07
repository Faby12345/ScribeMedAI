package ro.scribemed.backend.document.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ro.scribemed.backend.document.domain.ClinicalDocumentStatus;
import ro.scribemed.backend.document.domain.DocumentVersionSource;
import ro.scribemed.backend.document.domain.DocumentVersionStatus;
import ro.scribemed.backend.prescribedMedication.dto.PrescribedMedicationResponse;

public record DocumentReviewResponse(
        UUID consultationId,
        UUID documentId,
        String documentType,
        ClinicalDocumentStatus documentStatus,
        UUID versionId,
        int versionNumber,
        DocumentVersionStatus versionStatus,
        DocumentVersionSource source,
        SoapDraftResponse draft,
        List<String> reviewFlags,
        String aiProvider,
        String aiModel,
        String promptVersion,
        String templateVersion,
        Instant versionCreatedAt,
        List<PrescribedMedicationResponse> medications,
        TranscriptForReviewResponse transcript
) {

    public record SoapDraftResponse(
            String subjective,
            String objective,
            String assessment,
            String plan
    ) {
    }

    public record TranscriptForReviewResponse(
            String provider,
            String model,
            String language,
            String transcriptText,
            Instant createdAt
    ) {
    }
}
