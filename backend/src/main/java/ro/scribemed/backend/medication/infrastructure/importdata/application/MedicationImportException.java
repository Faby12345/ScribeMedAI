package ro.scribemed.backend.medication.infrastructure.importdata.application;

public class MedicationImportException extends RuntimeException {

    public MedicationImportException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}