package ro.scribemed.backend.transcription.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.transcription.domain.ConsultationTranscript;

public interface ConsultationTranscriptRepository extends JpaRepository<ConsultationTranscript, UUID> {

    Optional<ConsultationTranscript> findByConsultation_IdAndTenant_Id(UUID consultationId, UUID tenantId);

    boolean existsByConsultation_Id(UUID consultationId);
}
