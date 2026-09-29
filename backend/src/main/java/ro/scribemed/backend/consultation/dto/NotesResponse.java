package ro.scribemed.backend.consultation.dto;

import ro.scribemed.backend.processing.domain.ProcessingJobStatus;
import ro.scribemed.backend.processing.domain.ProcessingJobType;

import java.util.UUID;

public record NotesResponse(
        UUID notesId,
        UUID jobId,
        UUID consultationId,
        ProcessingJobStatus status
) {
}
