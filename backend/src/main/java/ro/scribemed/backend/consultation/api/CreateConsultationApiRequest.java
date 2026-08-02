package ro.scribemed.backend.consultation.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateConsultationApiRequest(
        @NotNull(message = "Pacientul este obligatoriu.")
        UUID patientId
) {
}
