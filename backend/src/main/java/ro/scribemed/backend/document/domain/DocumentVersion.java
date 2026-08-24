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
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "document_version")
public class DocumentVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private ClinicalDocument document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_notes_id")
    private ConsultationNotes sourceNotes;

    @Column(nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentVersionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentVersionSource source;

    @Column(columnDefinition = "text")
    private String subjective;

    @Column(columnDefinition = "text")
    private String objective;

    @Column(columnDefinition = "text")
    private String assessment;

    @Column(columnDefinition = "text")
    private String plan;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String reviewFlags;

    @Column(length = 100)
    private String aiProvider;

    @Column(length = 200)
    private String aiModel;

    @Column(length = 100)
    private String promptVersion;

    @Column(length = 100)
    private String templateVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private AppUser approvedByUser;

    @Column
    private Instant approvedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentVersion() {
    }

    public DocumentVersion(
            Tenant tenant,
            ClinicalDocument document,
            Consultation consultation,
            ConsultationNotes sourceNotes,
            int versionNumber,
            DocumentVersionSource source,
            String subjective,
            String objective,
            String assessment,
            String plan,
            String reviewFlags,
            String aiProvider,
            String aiModel,
            String promptVersion,
            String templateVersion,
            AppUser createdByUser
    ) {
        this.tenant = tenant;
        this.document = document;
        this.consultation = consultation;
        this.sourceNotes = sourceNotes;
        this.versionNumber = versionNumber;
        this.status = DocumentVersionStatus.DRAFT;
        this.source = source;
        this.subjective = subjective;
        this.objective = objective;
        this.assessment = assessment;
        this.plan = plan;
        this.reviewFlags = reviewFlags == null ? "[]" : reviewFlags;
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.promptVersion = promptVersion;
        this.templateVersion = templateVersion;
        this.createdByUser = createdByUser;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void markApproved(AppUser approvedByUser, Instant approvedAt) {
        status = DocumentVersionStatus.APPROVED;
        this.approvedByUser = approvedByUser;
        this.approvedAt = approvedAt;
    }

    public void markSuperseded() {
        status = DocumentVersionStatus.SUPERSEDED;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public ClinicalDocument getDocument() {
        return document;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public ConsultationNotes getSourceNotes() {
        return sourceNotes;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public DocumentVersionStatus getStatus() {
        return status;
    }

    public DocumentVersionSource getSource() {
        return source;
    }

    public String getSubjective() {
        return subjective;
    }

    public String getObjective() {
        return objective;
    }

    public String getAssessment() {
        return assessment;
    }

    public String getPlan() {
        return plan;
    }

    public String getReviewFlags() {
        return reviewFlags;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public AppUser getCreatedByUser() {
        return createdByUser;
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
}
