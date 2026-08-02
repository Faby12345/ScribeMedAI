package ro.scribemed.backend.transcription.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "consultation_transcript")
public class ConsultationTranscript {

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
    private String provider;

    @Column(nullable = false, length = 100)
    private String providerModel;

    @Column(length = 20)
    private String language;

    @Column(nullable = false)
    private String transcriptText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawProviderResponse;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ConsultationTranscript() {
    }

    public ConsultationTranscript(
            Tenant tenant,
            Consultation consultation,
            String provider,
            String providerModel,
            String language,
            String transcriptText,
            String rawProviderResponse
    ) {
        this.tenant = tenant;
        this.consultation = consultation;
        this.provider = provider;
        this.providerModel = providerModel;
        this.language = language;
        this.transcriptText = transcriptText;
        this.rawProviderResponse = rawProviderResponse;
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

    public String getProvider() {
        return provider;
    }

    public String getProviderModel() {
        return providerModel;
    }

    public String getLanguage() {
        return language;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
