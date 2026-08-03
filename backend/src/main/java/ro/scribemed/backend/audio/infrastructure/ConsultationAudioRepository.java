package ro.scribemed.backend.audio.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.audio.domain.ConsultationAudio;

public interface ConsultationAudioRepository extends JpaRepository<ConsultationAudio, UUID> {

    Optional<ConsultationAudio> findFirstByConsultation_IdAndTenant_IdOrderByCreatedAtDesc(
            UUID consultationId,
            UUID tenantId
    );
}
