package ro.scribemed.backend.knowledge.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocumentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentRepository
        extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByIdAndTenant_Id(UUID id, UUID tenantId);

    boolean existsByTenant_IdAndChecksum(UUID tenantId, String checksum);

    List<KnowledgeDocument> findAllByTenant_IdAndStatusOrderByCreatedAtDesc(
            UUID tenantId,
            KnowledgeDocumentStatus status
    );
}
