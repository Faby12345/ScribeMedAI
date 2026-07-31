package ro.scribemed.backend.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.audit.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByTenant_IdOrderByOccurredAtDesc(UUID tenantId);

    List<AuditEvent> findByTenant_IdAndActorUser_IdOrderByOccurredAtDesc(UUID tenantId, UUID actorUserId);

    List<AuditEvent> findByTenant_IdAndResourceTypeAndResourceIdOrderByOccurredAtDesc(
            UUID tenantId,
            String resourceType,
            UUID resourceId
    );
}
