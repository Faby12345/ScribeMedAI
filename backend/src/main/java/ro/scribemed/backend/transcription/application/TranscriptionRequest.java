package ro.scribemed.backend.transcription.application;

public record TranscriptionRequest(
        byte[] audioData,
        String contentType
) {
}
