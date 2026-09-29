package ro.scribemed.backend.transcription.application;

public record TranscriptionResult(
        String provider,
        String model,
        String language,
        String transcriptText,
        String rawProviderResponse
) {
}
