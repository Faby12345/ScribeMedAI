package ro.scribemed.backend.prescribedMedication.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.prescribedMedication.domain.PrescribedMedication;

import java.util.List;
import java.util.UUID;

public interface PrescribedMedicationRepository extends JpaRepository<PrescribedMedication, UUID> {

    List<PrescribedMedication> findByConsultationNotes_IdAndTenant_IdOrderByPositionAsc(
            UUID consultationNotesId,
            UUID tenantId
    );
}
