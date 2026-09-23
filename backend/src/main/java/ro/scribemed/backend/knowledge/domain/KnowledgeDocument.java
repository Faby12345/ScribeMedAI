package ro.scribemed.backend.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import ro.scribemed.backend.tenancy.domain.Tenant;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "source_institution", nullable = false, length = 255)
    private String sourceInstitution;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(length = 100)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KnowledgeDocumentStatus status;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(nullable = false, unique = true, length = 64)
    private String checksum;

    @Column(name = "object_key", nullable = false, unique = true, length = 1000)
    private String objectKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected KnowledgeDocument() {
        // Required by JPA
    }

    public KnowledgeDocument(
            Tenant tenant,
            String title,
            String sourceInstitution,
            String sourceUrl,
            LocalDate publishedAt,
            String version,
            String originalFilename,
            String checksum,
            String objectKey
    ) {
        this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
        this.title = title;
        this.sourceInstitution = sourceInstitution;
        this.sourceUrl = sourceUrl;
        this.publishedAt = publishedAt;
        this.version = version;
        this.originalFilename = originalFilename;
        this.checksum = checksum;
        this.objectKey = objectKey;
        this.status = KnowledgeDocumentStatus.PROCESSING;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markActive() {
        requireStatus(KnowledgeDocumentStatus.PROCESSING);
        status = KnowledgeDocumentStatus.ACTIVE;
    }

    public void markFailed() {
        requireStatus(KnowledgeDocumentStatus.PROCESSING);
        status = KnowledgeDocumentStatus.FAILED;
    }

    public void archive() {
        if (status == KnowledgeDocumentStatus.ARCHIVED) {
            return;
        }
        if (status == KnowledgeDocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "A document being processed cannot be archived"
            );
        }
        status = KnowledgeDocumentStatus.ARCHIVED;
    }

    private void requireStatus(KnowledgeDocumentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Knowledge document must have status " + expected
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceInstitution() {
        return sourceInstitution;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public String getVersion() {
        return version;
    }

    public KnowledgeDocumentStatus getStatus() {
        return status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
