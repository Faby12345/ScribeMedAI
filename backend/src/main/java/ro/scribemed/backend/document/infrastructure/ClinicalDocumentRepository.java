package ro.scribemed.backend.document.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.document.domain.ClinicalDocument;

public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, UUID> {

    Optional<ClinicalDocument> findByConsultation_IdAndTenant_IdAndDocumentType(
            UUID consultationId,
            UUID tenantId,
            String documentType
    );
}
