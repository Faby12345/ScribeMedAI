package ro.scribemed.backend.prescribedMedication.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "prescribed_medication")
public class PrescribedMedication {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_notes_id", nullable = false)
    private ConsultationNotes consultationNotes;

    @Column(nullable = false)
    private int position;

    @Column(name = "catalog_cim_code", nullable = false, length = 255)
    private String catalogCimCode;

    @Column(name = "commercial_name_snapshot", length = 255)
    private String commercialNameSnapshot;

    @Column(name = "active_substance_snapshot", length = 255)
    private String activeSubstanceSnapshot;

    @Column(name = "pharmaceutical_form_snapshot", length = 255)
    private String pharmaceuticalFormSnapshot;

    @Column(name = "concentration_snapshot", length = 255)
    private String concentrationSnapshot;

    @Column(name = "prescription_type_snapshot", length = 255)
    private String prescriptionTypeSnapshot;

    @Column(nullable = false, length = 255)
    private String dose;

    @Column(name = "administration_route", nullable = false, length = 100)
    private String administrationRoute;

    @Column(nullable = false, length = 255)
    private String frequency;

    @Column(nullable = false, length = 255)
    private String duration;

    @Column(length = 255)
    private String quantity;

    @Column(columnDefinition = "text")
    private String instructions;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PrescribedMedication() {
        // Required by JPA
    }

    public PrescribedMedication(
            Tenant tenant,
            ConsultationNotes consultationNotes,
            int position,
            String catalogCimCode,
            String commercialNameSnapshot,
            String activeSubstanceSnapshot,
            String pharmaceuticalFormSnapshot,
            String concentrationSnapshot,
            String prescriptionTypeSnapshot,
            String dose,
            String administrationRoute,
            String frequency,
            String duration,
            String quantity,
            String instructions,
            String notes
    ) {
        this.tenant = tenant;
        this.consultationNotes = consultationNotes;
        this.position = position;
        this.catalogCimCode = catalogCimCode;
        this.commercialNameSnapshot = commercialNameSnapshot;
        this.activeSubstanceSnapshot = activeSubstanceSnapshot;
        this.pharmaceuticalFormSnapshot = pharmaceuticalFormSnapshot;
        this.concentrationSnapshot = concentrationSnapshot;
        this.prescriptionTypeSnapshot = prescriptionTypeSnapshot;
        this.dose = dose;
        this.administrationRoute = administrationRoute;
        this.frequency = frequency;
        this.duration = duration;
        this.quantity = quantity;
        this.instructions = instructions;
        this.notes = notes;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public ConsultationNotes getConsultationNotes() {
        return consultationNotes;
    }

    public int getPosition() {
        return position;
    }

    public String getCatalogCimCode() {
        return catalogCimCode;
    }

    public String getCommercialNameSnapshot() {
        return commercialNameSnapshot;
    }

    public String getActiveSubstanceSnapshot() {
        return activeSubstanceSnapshot;
    }

    public String getPharmaceuticalFormSnapshot() {
        return pharmaceuticalFormSnapshot;
    }

    public String getConcentrationSnapshot() {
        return concentrationSnapshot;
    }

    public String getPrescriptionTypeSnapshot() {
        return prescriptionTypeSnapshot;
    }

    public String getDose() {
        return dose;
    }

    public String getAdministrationRoute() {
        return administrationRoute;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getDuration() {
        return duration;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
