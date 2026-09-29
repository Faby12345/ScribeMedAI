package ro.scribemed.backend.medication.infrastructure.importdata.application;

import org.springframework.stereotype.Component;
import ro.scribemed.backend.medication.infrastructure.importdata.configuration.MedicationImportRow;

@Component
public class MedicationImportValidator {

    public boolean isValid(MedicationImportRow row) {
        return hasText(row.cimCode())
                && hasText(row.commercialName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}