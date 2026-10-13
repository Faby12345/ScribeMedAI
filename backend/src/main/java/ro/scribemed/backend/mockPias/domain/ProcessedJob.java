package ro.scribemed.backend.mockPias.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ro.scribemed.backend.tenancy.domain.Tenant;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "processed_kafka_job",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_processed_kafka_job_consumer_idempotency",
                columnNames = {"tenant_id", "consumer_name", "idempotency_key"}
        )
)
public class ProcessedJob {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "consumer_name", nullable = false, updatable = false, length = 100)
    private String consumerName;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedJob() {
    }

    public ProcessedJob(
            Tenant tenant,
            UUID jobId,
            String consumerName,
            String idempotencyKey
    ) {
        this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
        this.jobId = Objects.requireNonNull(jobId, "jobId must not be null");
        this.consumerName = requireText(consumerName, "consumerName");
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    }

    @PrePersist
    void prePersist() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public UUID getJobId() {
        return jobId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
