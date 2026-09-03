package ro.scribemed.backend.shared.rateLimit.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.scribemed.backend.shared.rateLimit.domain.RateLimitScope;
import ro.scribemed.backend.shared.rateLimit.domain.RateLimitWindow;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateLimitWindowRepository
        extends JpaRepository<RateLimitWindow, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO rate_limit_window (
            id,
            scope,
            identifier,
            window_started_at,
            request_count,
            updated_at
        )
        VALUES (
            :id,
            :scope,
            :identifier,
            :now,
            0,
            :now
        )
        ON CONFLICT (scope, identifier) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("scope") String scope,
            @Param("identifier") String identifier,
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select w
        from RateLimitWindow w
        where w.scope = :scope
          and w.identifier = :identifier
        """)
    Optional<RateLimitWindow> findByScopeAndIdentifier(
            @Param("scope") RateLimitScope scope,
            @Param("identifier") String identifier
    );
}