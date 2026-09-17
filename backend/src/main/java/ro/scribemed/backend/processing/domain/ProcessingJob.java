package ro.scribemed.backend.processing.domain;

import java.time.Instant;
import java.util.Objects;
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
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.tenancy.domain.Tenant;

/*
Simptome debutate progresiv, fără dispnee, durere toracică sau
alergii medicamentoase cunoscute.
 A administrat paracetamol ocazional, cu ameliorare parțială.
 */

/*
* Pacientul se prezintă pentru tuse seacă,
* rinoree și subfebrilitate apărute de aproximativ 3 zile.*/

@Entity
@Table(name = "processing_job")
public class ProcessingJob {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_document_id")
    private KnowledgeDocument knowledgeDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_notes_id")
    private ConsultationNotes sourceNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProcessingJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private ProcessingJobTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProcessingJobStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column
    private Instant lockedAt;

    @Column(length = 100)
    private String lockedBy;

    @Column(length = 100)
    private String errorCode;

    @Column(length = 500)
    private String errorMessageSanitized;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    protected ProcessingJob() {
    }

    public ProcessingJob(Tenant tenant, Consultation consultation, ProcessingJobType jobType) {
        initialize(tenant, jobType, ProcessingJobTargetType.CONSULTATION);
        this.consultation = Objects.requireNonNull(
                consultation,
                "consultation must not be null"
        );
    }

    public ProcessingJob(
            Tenant tenant,
            KnowledgeDocument knowledgeDocument,
            ProcessingJobType jobType
    ) {
        initialize(tenant, jobType, ProcessingJobTargetType.KNOWLEDGE_DOCUMENT);
        this.knowledgeDocument = Objects.requireNonNull(
                knowledgeDocument,
                "knowledgeDocument must not be null"
        );
    }

    public ProcessingJob(
            Tenant tenant,
            Consultation consultation,
            ConsultationNotes sourceNotes,
            ProcessingJobType jobType
    ) {
        this(tenant, consultation, jobType);
        this.sourceNotes = Objects.requireNonNull(
                sourceNotes,
                "sourceNotes must not be null"
        );
    }

    private void initialize(
            Tenant tenant,
            ProcessingJobType jobType,
            ProcessingJobTargetType expectedTargetType
    ) {
        this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
        this.jobType = Objects.requireNonNull(jobType, "jobType must not be null");
        if (jobType.targetType() != expectedTargetType) {
            throw new IllegalArgumentException(
                    "Job type %s requires target type %s"
                            .formatted(jobType, jobType.targetType())
            );
        }

        this.targetType = expectedTargetType;
        this.status = ProcessingJobStatus.PENDING;
        this.maxAttempts = 3;
        this.nextAttemptAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void markRunning(String workerId, Instant now) {
        status = ProcessingJobStatus.RUNNING;
        attemptCount++;
        lockedAt = now;
        lockedBy = workerId;
        startedAt = startedAt == null ? now : startedAt;
        errorCode = null;
        errorMessageSanitized = null;
    }

    public void markSucceeded(Instant now) {
        status = ProcessingJobStatus.SUCCEEDED;
        completedAt = now;
        lockedAt = null;
        lockedBy = null;
    }

    public void markFailed(String errorCode, String safeMessage, Instant now) {
        status = ProcessingJobStatus.FAILED;
        completedAt = now;
        lockedAt = null;
        lockedBy = null;
        this.errorCode = errorCode;
        errorMessageSanitized = safeMessage;
    }

    public void markRetry(String errorCode, String safeMessage, Instant nextAttemptAt) {
        status = ProcessingJobStatus.RETRY;
        lockedAt = null;
        lockedBy = null;
        this.errorCode = errorCode;
        errorMessageSanitized = safeMessage;
        this.nextAttemptAt = nextAttemptAt;
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts;
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

    public KnowledgeDocument getKnowledgeDocument() {
        return knowledgeDocument;
    }

    public ConsultationNotes getSourceNotes() {
        return sourceNotes;
    }

    public ProcessingJobType getJobType() {
        return jobType;
    }

    public ProcessingJobTargetType getTargetType() {
        return targetType;
    }

    public ProcessingJobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
