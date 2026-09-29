package ro.scribemed.backend.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserStatus;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByEmailAndStatus(String email, UserStatus status);

    Optional<AppUser> findByTenant_IdAndEmail(UUID tenantId, String email);

    Optional<AppUser> findByIdAndTenant_Id(UUID id, UUID tenantId);

    boolean existsByTenant_IdAndEmail(UUID tenantId, String email);

    boolean existsByTenant_IdAndEmailAndStatus(UUID tenantId, String email, UserStatus status);
}
