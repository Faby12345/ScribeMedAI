package ro.scribemed.backend.document.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "clinical_document")
public class ClinicalDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column(nullable = false, length = 50)
    private String documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClinicalDocumentStatus status;

    @Column(nullable = false)
    private int currentVersionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private AppUser approvedByUser;

    @Column
    private Instant approvedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ClinicalDocument() {
    }

    public ClinicalDocument(Tenant tenant, Consultation consultation, String documentType) {
        this.tenant = tenant;
        this.consultation = consultation;
        this.documentType = documentType;
        this.status = ClinicalDocumentStatus.DRAFT;
        this.currentVersionNumber = 0;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public int nextVersionNumber() {
        currentVersionNumber++;
        return currentVersionNumber;
    }

    public void markApproved(AppUser approvedByUser, Instant approvedAt) {
        this.status = ClinicalDocumentStatus.APPROVED;
        this.approvedByUser = approvedByUser;
        this.approvedAt = approvedAt;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public String getDocumentType() {
        return documentType;
    }

    public ClinicalDocumentStatus getStatus() {
        return status;
    }

    public int getCurrentVersionNumber() {
        return currentVersionNumber;
    }

    public AppUser getApprovedByUser() {
        return approvedByUser;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
