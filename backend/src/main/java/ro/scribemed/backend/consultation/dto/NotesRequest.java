package ro.scribemed.backend.consultation.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.scribemed.backend.prescribedMedication.dto.PrescribedMedicationRequest;

public record NotesRequest (
        String reason,
        String assessment,
        String history,
        String objective,
        String plan,
        @Size(max = 20, message = "Pot fi adăugate maximum 20 de medicamente.")
        List<@Valid @NotNull(message = "Medicamentul nu poate fi gol.") PrescribedMedicationRequest> medications
) {
    public NotesRequest {
        medications = medications == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(medications));
    }
}
