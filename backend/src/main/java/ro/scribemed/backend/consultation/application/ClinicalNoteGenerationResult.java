package ro.scribemed.backend.consultation.application;

import java.util.List;

public record ClinicalNoteGenerationResult(
        String provider,
        String model,
        String promptVersion,
        String templateVersion,
        SoapNote soapNote,
        List<String> reviewFlags,
        String rawProviderResponse
) {
    public record SoapNote(
            String subjective,
            String objective,
            String assessment,
            String plan
    ) {
    }
}
