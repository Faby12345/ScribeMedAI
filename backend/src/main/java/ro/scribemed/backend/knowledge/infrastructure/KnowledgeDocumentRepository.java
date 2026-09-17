package ro.scribemed.backend.knowledge.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocumentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentRepository
        extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByChecksum(String checksum);

    boolean existsByChecksum(String checksum);

    List<KnowledgeDocument> findAllByStatusOrderByCreatedAtDesc(
            KnowledgeDocumentStatus status
    );
}
