package ro.scribemed.backend.prescribedMedication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescribedMedicationRequest(
        @NotBlank(message = "Codul CIM este obligatoriu.")
        @Size(max = 255, message = "Codul CIM este prea lung.")
        String cimCode,

        @NotBlank(message = "Doza este obligatorie.")
        @Size(max = 255, message = "Doza este prea lungă.")
        String dose,

        @NotBlank(message = "Calea de administrare este obligatorie.")
        @Size(max = 100, message = "Calea de administrare este prea lungă.")
        String administrationRoute,

        @NotBlank(message = "Frecvența este obligatorie.")
        @Size(max = 255, message = "Frecvența este prea lungă.")
        String frequency,

        @NotBlank(message = "Durata este obligatorie.")
        @Size(max = 255, message = "Durata este prea lungă.")
        String duration,

        @Size(max = 255, message = "Cantitatea este prea lungă.")
        String quantity,

        @Size(max = 4000, message = "Instrucțiunile sunt prea lungi.")
        String instructions,

        @Size(max = 4000, message = "Observațiile sunt prea lungi.")
        String notes
) {
}
