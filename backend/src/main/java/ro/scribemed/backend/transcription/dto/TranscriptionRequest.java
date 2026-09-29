package ro.scribemed.backend.transcription.dto;

public record TranscriptionRequest(
        byte[] audioData,
        String contentType
) {
}
