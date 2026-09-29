package ro.scribemed.backend.consultation.dto;

import ro.scribemed.backend.consultation.domain.ConsultationNotes;

public record NotesRequest (
        String reason,
        String assessment,
        String history,
        String objective,
        String plan
) {
}

