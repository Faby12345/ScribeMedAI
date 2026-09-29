package ro.scribemed.backend.identity.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.scribemed.backend.identity.domain.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findBySessionTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
            String sessionTokenHash,
            Instant now
    );

    @Query("""
            select session
            from UserSession session
            join fetch session.tenant
            join fetch session.user
            where session.sessionTokenHash = :sessionTokenHash
              and session.revokedAt is null
              and session.expiresAt > :now
            """)
    Optional<UserSession> findActiveBySessionTokenHashWithUserAndTenant(
            @Param("sessionTokenHash") String sessionTokenHash,
            @Param("now") Instant now
    );

    List<UserSession> findByUser_IdAndRevokedAtIsNull(UUID userId);

    List<UserSession> findByTenant_IdAndRevokedAtIsNull(UUID tenantId);
}
