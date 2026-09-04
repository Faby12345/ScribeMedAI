package ro.scribemed.backend.medication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "medication")
public class Medication {

    @Id
    @Column(name = "cim_code", nullable = false)
    private String cimCode;

    @Column(name = "commercial_name")
    private String commercialName;

    @Column(name = "active_substance")
    private String activeSubstance;

    @Column(name = "pharmaceutical_form")
    private String pharmaceuticalForm;

    @Column(name = "concentration")
    private String concentration;

    @Column(name = "app_manufacturer")
    private String appManufacturer;

    @Column(name = "app_holder")
    private String appHolder;

    @Column(name = "atc_code")
    private String atcCode;

    @Column(name = "therapeutic_action")
    private String therapeuticAction;

    @Column(name = "prescription_type")
    private String prescriptionType;

    @Column(name = "app_packaging_authorization")
    private String appPackagingAuthorization;

    @Column(name = "packaging")
    private String packaging;

    @Column(name = "packaging_volume")
    private String packagingVolume;

    @Column(name = "packaging_validity")
    private String packagingValidity;

    @Column(name = "centralized_pending_romanian_decision")
    private boolean centralizedPendingRomanianDecision;

    @Column(name = "temporary_circulation")
    private boolean temporaryCirculation;

    @Column(name = "centralized_authorized")
    private boolean centralizedAuthorized;

    @Column(name = "authorization_suspended")
    private boolean authorizationSuspended;

    @Column(name = "has_additional_information")
    private boolean hasAdditionalInformation;

    @Column(name = "source_updated_at")
    private LocalDate sourceUpdatedAt;

    protected Medication() {
        // Required by JPA
    }

    public Medication(
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
        this.cimCode = cimCode;
        this.commercialName = commercialName;
        this.activeSubstance = activeSubstance;
        this.pharmaceuticalForm = pharmaceuticalForm;
        this.concentration = concentration;
        this.appManufacturer = appManufacturer;
        this.appHolder = appHolder;
        this.atcCode = atcCode;
        this.therapeuticAction = therapeuticAction;
        this.prescriptionType = prescriptionType;
        this.appPackagingAuthorization = appPackagingAuthorization;
        this.packaging = packaging;
        this.packagingVolume = packagingVolume;
        this.packagingValidity = packagingValidity;
        this.centralizedPendingRomanianDecision =
                centralizedPendingRomanianDecision;
        this.temporaryCirculation = temporaryCirculation;
        this.centralizedAuthorized = centralizedAuthorized;
        this.authorizationSuspended = authorizationSuspended;
        this.hasAdditionalInformation = hasAdditionalInformation;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public String getCimCode() {
        return cimCode;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public String getActiveSubstance() {
        return activeSubstance;
    }

    public String getPharmaceuticalForm() {
        return pharmaceuticalForm;
    }

    public String getConcentration() {
        return concentration;
    }

    public String getAppManufacturer() {
        return appManufacturer;
    }

    public String getAppHolder() {
        return appHolder;
    }

    public String getAtcCode() {
        return atcCode;
    }

    public String getTherapeuticAction() {
        return therapeuticAction;
    }

    public String getPrescriptionType() {
        return prescriptionType;
    }

    public String getAppPackagingAuthorization() {
        return appPackagingAuthorization;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getPackagingVolume() {
        return packagingVolume;
    }

    public String getPackagingValidity() {
        return packagingValidity;
    }

    public boolean isCentralizedPendingRomanianDecision() {
        return centralizedPendingRomanianDecision;
    }

    public boolean isTemporaryCirculation() {
        return temporaryCirculation;
    }

    public boolean isCentralizedAuthorized() {
        return centralizedAuthorized;
    }

    public boolean isAuthorizationSuspended() {
        return authorizationSuspended;
    }

    public boolean hasAdditionalInformation() {
        return hasAdditionalInformation;
    }

    public LocalDate getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }
}
