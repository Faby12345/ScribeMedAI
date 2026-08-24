package ro.scribemed.backend.consultation.domain;

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
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "consultation_notes")
public class ConsultationNotes {

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
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdByUser;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(columnDefinition = "text")
    private String history;

    @Column(columnDefinition = "text")
    private String objective;

    @Column(columnDefinition = "text")
    private String assessment;

    @Column(columnDefinition = "text")
    private String plan;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ConsultationNotes() {
    }

    public ConsultationNotes(
            Tenant tenant,
            Consultation consultation,
            AppUser createdByUser,
            String reason,
            String history,
            String objective,
            String assessment,
            String plan
    ) {
        this.tenant = tenant;
        this.consultation = consultation;
        this.createdByUser = createdByUser;
        this.reason = reason;
        this.history = history;
        this.objective = objective;
        this.assessment = assessment;
        this.plan = plan;
    }
    public static ConsultationNotes create(
            Tenant tenant,
            Consultation consultation,
            AppUser createdByUser,
            String reason,
            String history,
            String objective,
            String assessment,
            String plan
    ) {
        return new ConsultationNotes(
                tenant,
                consultation,
                createdByUser,
                reason,
                history,
                objective,
                assessment,
                plan
        );
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

    public Consultation getConsultation() {
        return consultation;
    }

    public AppUser getCreatedByUser() {
        return createdByUser;
    }

    public String getReason() {
        return reason;
    }

    public String getHistory() {
        return history;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
