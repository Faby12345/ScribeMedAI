package ro.scribemed.backend.consultation.application;

import java.time.Instant;
import java.util.UUID;

import ro.scribemed.backend.transcription.domain.ConsultationTranscript;

public record TranscriptResponse(
        UUID consultationId,
        String provider,
        String model,
        String language,
        String transcriptText,
        Instant createdAt
) {

    public static TranscriptResponse from(ConsultationTranscript transcript) {
        return new TranscriptResponse(
                transcript.getConsultation().getId(),
                transcript.getProvider(),
                transcript.getProviderModel(),
                transcript.getLanguage(),
                transcript.getTranscriptText(),
                transcript.getCreatedAt()
        );
    }
}
