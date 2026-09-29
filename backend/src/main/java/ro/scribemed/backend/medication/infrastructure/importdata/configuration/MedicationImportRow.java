package ro.scribemed.backend.medication.infrastructure.importdata.configuration;

import java.time.LocalDate;

public record MedicationImportRow(
        String cimCode,
        String commercialName,
        String activeSubstance,
        String pharmaceuticalForm,
        String concentration,
        String appManufacturer,
        String appHolder,
        String atcCode,
        String therapeuticAction,
        String prescriptionType,
        String appPackagingAuthorization,
        String packaging,
        String packagingVolume,
        String packagingValidity,
        boolean centralizedPendingRomanianDecision,
        boolean temporaryCirculation,
        boolean centralizedAuthorized,
        boolean authorizationSuspended,
        boolean hasAdditionalInformation,
        LocalDate sourceUpdatedAt
) {
}
