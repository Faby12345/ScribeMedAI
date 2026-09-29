package ro.scribemed.backend.document.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.domain.DocumentVersionStatus;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    boolean existsBySourceNotes_IdAndTenant_Id(UUID sourceNotesId, UUID tenantId);

    Optional<DocumentVersion> findFirstByDocument_IdAndTenant_IdOrderByVersionNumberDesc(
            UUID documentId,
            UUID tenantId
    );

    List<DocumentVersion> findByDocument_IdAndTenant_IdOrderByVersionNumberDesc(
            UUID documentId,
            UUID tenantId
    );

    Optional<DocumentVersion> findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
            UUID documentId,
            UUID tenantId,
            DocumentVersionStatus status
    );
}
