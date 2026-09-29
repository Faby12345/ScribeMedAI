package ro.scribemed.backend.audit.application;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.audit.domain.AuditEvent;
import ro.scribemed.backend.audit.infrastructure.AuditEventRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(
            Tenant tenant,
            AppUser actorUser,
            String eventType,
            String resourceType,
            UUID resourceId,
            Map<String, Object> metadata
    ) {
        auditEventRepository.save(new AuditEvent(
                tenant,
                actorUser,
                eventType,
                resourceType,
                resourceId,
                metadata
        ));
    }
}
