package ro.scribemed.backend.knowledge.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.scribemed.backend.knowledge.domain.KnowledgeChunk;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocumentStatus;

import java.util.List;
import java.util.UUID;

public interface KnowledgeChunkRepository
        extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findAllByTenant_IdAndDocument_IdOrderByChunkIndex(
            UUID tenantId,
            UUID documentId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from KnowledgeChunk chunk
            where chunk.tenant.id = :tenantId
              and chunk.document.id = :documentId
            """)
    int deleteAllByTenantIdAndDocumentId(
            @Param("tenantId") UUID tenantId,
            @Param("documentId") UUID documentId
    );

    @Query("""
            select chunk
            from KnowledgeChunk chunk
            join fetch chunk.document document
            where chunk.tenant.id = :tenantId
              and document.tenant.id = :tenantId
              and document.status = :status
            order by cosine_distance(chunk.embedding, :embedding)
            """)
    List<KnowledgeChunk> findNearestByCosineDistance(
            @Param("tenantId") UUID tenantId,
            @Param("embedding") float[] embedding,
            @Param("status") KnowledgeDocumentStatus status,
            Pageable pageable
    );

    @Query("""
          select chunk
          from KnowledgeChunk chunk
          join fetch chunk.document document
          where chunk.tenant.id = :tenantId
            and document.tenant.id = :tenantId
            and document.status = :status
            and document.id in :documentIds
          order by cosine_distance(chunk.embedding, :embedding)
          """)
    List<KnowledgeChunk> findNearestByCosineDistanceAndDocumentIds(
            @Param("tenantId") UUID tenantId,
            @Param("documentIds") List<UUID> documentIds,
            @Param("embedding") float[] embedding,
            @Param("status") KnowledgeDocumentStatus status,
            Pageable pageable
    );
}
