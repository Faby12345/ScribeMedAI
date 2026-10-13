package ro.scribemed.backend.medicationdraft.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
import jakarta.persistence.Version;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.medication.domain.Medication;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.transcription.domain.ConsultationTranscript;

@Entity
@Table(name = "medication_draft_candidate")
public class MedicationDraftCandidate {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcript_id", nullable = false)
    private ConsultationTranscript transcript;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "mention_type", nullable = false, length = 30)
    private MedicationMentionType mentionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 40)
    private MedicationReviewStatus reviewStatus;

    @Column(name = "extracted_name", nullable = false, length = 255)
    private String extractedName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_cim_code")
    private Medication catalogMedication;

    @Column(length = 255)
    private String dose;

    @Column(name = "administration_route", length = 100)
    private String administrationRoute;

    @Column(length = 255)
    private String frequency;

    @Column(length = 255)
    private String duration;

    @Column(length = 255)
    private String quantity;

    @Column(columnDefinition = "text")
    private String instructions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_segment_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> sourceSegmentIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private AppUser reviewedByUser;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MedicationDraftCandidate() {
        // Required by JPA
    }

    public MedicationDraftCandidate(
            Tenant tenant,
            Consultation consultation,
            DocumentVersion documentVersion,
            ConsultationTranscript transcript,
            int position,
            MedicationMentionType mentionType,
            String extractedName,
            Medication catalogMedication,
            String dose,
            String administrationRoute,
            String frequency,
            String duration,
            String quantity,
            String instructions,
            List<String> sourceSegmentIds
    ) {
        this.tenant = tenant;
        this.consultation = consultation;
        this.documentVersion = documentVersion;
        this.transcript = transcript;
        this.position = position;
        this.mentionType = mentionType;
        this.reviewStatus = MedicationReviewStatus.PENDING_REVIEW;
        this.extractedName = extractedName;
        this.catalogMedication = catalogMedication;
        this.dose = dose;
        this.administrationRoute = administrationRoute;
        this.frequency = frequency;
        this.duration = duration;
        this.quantity = quantity;
        this.instructions = instructions;
        this.sourceSegmentIds = sourceSegmentIds == null ? List.of() : List.copyOf(sourceSegmentIds);
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

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public DocumentVersion getDocumentVersion() {
        return documentVersion;
    }

    public ConsultationTranscript getTranscript() {
        return transcript;
    }

    public int getPosition() {
        return position;
    }

    public MedicationMentionType getMentionType() {
        return mentionType;
    }

    public MedicationReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public String getExtractedName() {
        return extractedName;
    }

    public Medication getCatalogMedication() {
        return catalogMedication;
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

    public List<String> getSourceSegmentIds() {
        return List.copyOf(sourceSegmentIds);
    }

    public AppUser getReviewedByUser() {
        return reviewedByUser;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
