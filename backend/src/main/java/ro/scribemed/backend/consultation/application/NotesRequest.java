package ro.scribemed.backend.consultation.application;

public record NotesRequest (
        String reason,
        String assessment,
        String history,
        String objective,
        String plan
) {
}
