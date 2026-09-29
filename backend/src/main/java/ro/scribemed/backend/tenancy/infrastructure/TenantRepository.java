package ro.scribemed.backend.tenancy.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    List<Tenant> findByStatus(TenantStatus status);

    Optional<Tenant> findByName(String name);
}
