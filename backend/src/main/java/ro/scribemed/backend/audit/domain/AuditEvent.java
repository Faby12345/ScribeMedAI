package ro.scribemed.backend.audit.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actorUser;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(length = 100)
    private String resourceType;

    @Column
    private UUID resourceId;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    protected AuditEvent() {
    }

    public AuditEvent(
            Tenant tenant,
            AppUser actorUser,
            String eventType,
            String resourceType,
            UUID resourceId,
            Map<String, Object> metadata
    ) {
        this.tenant = tenant;
        this.actorUser = actorUser;
        this.eventType = eventType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }

    @PrePersist
    void prePersist() {
        occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AppUser getActorUser() {
        return actorUser;
    }

    public String getEventType() {
        return eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }
}
