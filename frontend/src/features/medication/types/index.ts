export type MedicationResponse = {
    cimCode: string;
    commercialName: string | null;
    activeSubstance: string | null;
    pharmaceuticalForm: string | null;
    concentration: string | null;
    appManufacturer: string | null;
    appHolder: string | null;
    atcCode: string | null;
    therapeuticAction: string | null;
    prescriptionType: string | null;
    appPackagingAuthorization: string | null;
    packaging: string | null;
    packagingVolume: string | null;
    packagingValidity: string | null;
    centralizedPendingRomanianDecision: boolean;
    temporaryCirculation: boolean;
    centralizedAuthorized: boolean;
    authorizationSuspended: boolean;
    hasAdditionalInformation: boolean;
    sourceUpdatedAt: string | null; // ISO date: YYYY-MM-DD
};
