package ro.scribemed.backend.medicationdraft.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.medicationdraft.domain.MedicationDraftCandidate;
import ro.scribemed.backend.medicationdraft.domain.MedicationReviewStatus;

public interface MedicationDraftCandidateRepository
        extends JpaRepository<MedicationDraftCandidate, UUID> {

    List<MedicationDraftCandidate> findByDocumentVersion_IdAndTenant_IdOrderByPositionAsc(
            UUID documentVersionId,
            UUID tenantId
    );

    Optional<MedicationDraftCandidate> findByIdAndTenant_Id(UUID candidateId, UUID tenantId);

    boolean existsByDocumentVersion_IdAndTenant_IdAndReviewStatus(
            UUID documentVersionId,
            UUID tenantId,
            MedicationReviewStatus reviewStatus
    );
}
