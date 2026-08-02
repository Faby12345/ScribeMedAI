package ro.scribemed.backend.consultation.application;

import java.util.UUID;

import ro.scribemed.backend.consultation.domain.ConsultationStatus;

public record AudioUploadResponse(
        UUID consultationId,
        UUID audioId,
        UUID jobId,
        ConsultationStatus status
) {
}
