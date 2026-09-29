package ro.scribemed.backend.audio.domain;

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
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "consultation_audio")
public class ConsultationAudio {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column(nullable = false, length = 500)
    private String objectKey;

    @Column(length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConsultationAudioStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ConsultationAudio() {
    }

    public ConsultationAudio(
            Tenant tenant,
            Consultation consultation,
            String objectKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksumSha256
    ) {
        this.tenant = tenant;
        this.consultation = consultation;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.status = ConsultationAudioStatus.STORED;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void markTranscribed() {
        status = ConsultationAudioStatus.TRANSCRIBED;
    }

    public void markFailed() {
        status = ConsultationAudioStatus.FAILED;
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

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public ConsultationAudioStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
