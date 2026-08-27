package ro.scribemed.backend.shared.rateLimit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.scribemed.backend.shared.rateLimit.domain.RateLimitWindow;

import java.util.UUID;

@Repository
public interface RateLimitWindowRepository extends JpaRepository<UUID, RateLimitWindow> {
}
