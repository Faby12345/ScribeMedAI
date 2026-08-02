package ro.scribemed.backend.consultation.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.consultation.domain.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    Optional<Consultation> findByIdAndTenant_Id(UUID id, UUID tenantId);
}
