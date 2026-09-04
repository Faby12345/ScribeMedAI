package ro.scribemed.backend.medication.infrastructure.importdata.application;

public record MedicationImportResult(
        int total,
        int written,
        int skipped
) {
}
