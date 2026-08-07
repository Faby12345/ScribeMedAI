package ro.scribemed.backend.consultation.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;

public interface ConsultationNotesRepository extends JpaRepository<ConsultationNotes, UUID> {

    Optional<ConsultationNotes> findFirstByConsultation_IdAndTenant_IdOrderByCreatedAtDesc(
            UUID consultationId,
            UUID tenantId
    );
}
