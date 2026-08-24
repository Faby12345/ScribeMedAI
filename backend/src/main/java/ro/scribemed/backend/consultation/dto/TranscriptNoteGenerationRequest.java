package ro.scribemed.backend.consultation.dto;

public record TranscriptNoteGenerationRequest(
        String transcriptText,
        String language,
        String transcriptionProvider,
        String transcriptionModel
) {
}
