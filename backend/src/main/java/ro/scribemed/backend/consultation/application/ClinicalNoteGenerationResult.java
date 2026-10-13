package ro.scribemed.backend.consultation.application;

import java.util.List;

import ro.scribemed.backend.medicationdraft.domain.MedicationMentionType;

public record ClinicalNoteGenerationResult(
        String provider,
        String model,
        String promptVersion,
        String templateVersion,
        SoapNote soapNote,
        List<MedicationCandidate> medications,
        List<String> reviewFlags,
        String rawProviderResponse
) {
    public ClinicalNoteGenerationResult {
        medications = medications == null ? List.of() : List.copyOf(medications);
        reviewFlags = reviewFlags == null ? List.of() : List.copyOf(reviewFlags);
    }

    public record SoapNote(
            String subjective,
            String objective,
            String assessment,
            String plan
    ) {
    }

    public record MedicationCandidate(
            MedicationMentionType mentionType,
            String extractedName,
            String dose,
            String administrationRoute,
            String frequency,
            String duration,
            String quantity,
            String instructions,
            List<String> sourceSegmentIds
    ) {
        public MedicationCandidate {
            sourceSegmentIds = sourceSegmentIds == null
                    ? List.of()
                    : List.copyOf(sourceSegmentIds);
        }
    }
}
