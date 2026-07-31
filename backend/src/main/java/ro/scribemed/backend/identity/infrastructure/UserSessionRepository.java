package ro.scribemed.backend.identity.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.identity.domain.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findBySessionTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
            String sessionTokenHash,
            Instant now
    );

    List<UserSession> findByUser_IdAndRevokedAtIsNull(UUID userId);

    List<UserSession> findByTenant_IdAndRevokedAtIsNull(UUID tenantId);
}
