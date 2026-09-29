package ro.scribemed.backend.knowledge.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocumentStatus;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentRepository
        extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByIdAndTenant_Id(UUID id, UUID tenantId);

    boolean existsByTenant_IdAndChecksum(UUID tenantId, String checksum);

    Page<KnowledgeDocument> findAllByTenant_IdAndStatus(
            UUID tenantId,
            KnowledgeDocumentStatus status,
            Pageable pageable
    );
}
