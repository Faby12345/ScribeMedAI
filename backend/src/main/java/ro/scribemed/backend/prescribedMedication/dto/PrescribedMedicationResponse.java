package ro.scribemed.backend.prescribedMedication.dto;

import java.util.UUID;

import ro.scribemed.backend.prescribedMedication.domain.PrescribedMedication;

public record PrescribedMedicationResponse(
        UUID id,
        int position,
        String cimCode,
        String commercialName,
        String activeSubstance,
        String pharmaceuticalForm,
        String concentration,
        String prescriptionType,
        String dose,
        String administrationRoute,
        String frequency,
        String duration,
        String quantity,
        String instructions,
        String notes
) {
    public static PrescribedMedicationResponse from(PrescribedMedication medication) {
        return new PrescribedMedicationResponse(
                medication.getId(),
                medication.getPosition(),
                medication.getCatalogCimCode(),
                medication.getCommercialNameSnapshot(),
                medication.getActiveSubstanceSnapshot(),
                medication.getPharmaceuticalFormSnapshot(),
                medication.getConcentrationSnapshot(),
                medication.getPrescriptionTypeSnapshot(),
                medication.getDose(),
                medication.getAdministrationRoute(),
                medication.getFrequency(),
                medication.getDuration(),
                medication.getQuantity(),
                medication.getInstructions(),
                medication.getNotes()
        );
    }
}
