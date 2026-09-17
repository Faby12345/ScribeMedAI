package ro.scribemed.backend.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk {

    public static final int EMBEDDING_DIMENSIONS = 384;

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, updatable = false)
    private KnowledgeDocument document;

    @Column(name = "chunk_index", nullable = false, updatable = false)
    private int chunkIndex;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String content;

    @Column(name = "page_from", updatable = false)
    private Integer pageFrom;

    @Column(name = "page_to", updatable = false)
    private Integer pageTo;

    @Column(name = "section_title", length = 500, updatable = false)
    private String sectionTitle;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EMBEDDING_DIMENSIONS)
    @Column(nullable = false, updatable = false, columnDefinition = "vector(384)")
    private float[] embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected KnowledgeChunk() {
        // Required by JPA
    }

    public KnowledgeChunk(
            KnowledgeDocument document,
            int chunkIndex,
            String content,
            Integer pageFrom,
            Integer pageTo,
            String sectionTitle,
            float[] embedding
    ) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.sectionTitle = sectionTitle;
        this.embedding = Arrays.copyOf(embedding, embedding.length);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public KnowledgeDocument getDocument() {
        return document;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public Integer getPageFrom() {
        return pageFrom;
    }

    public Integer getPageTo() {
        return pageTo;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public float[] getEmbedding() {
        return Arrays.copyOf(embedding, embedding.length);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
