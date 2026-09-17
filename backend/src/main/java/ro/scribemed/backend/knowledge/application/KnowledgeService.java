package ro.scribemed.backend.knowledge.application;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.knowledge.domain.KnowledgeChunk;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocumentStatus;
import ro.scribemed.backend.knowledge.infrastructure.KnowledgeChunkRepository;
import ro.scribemed.backend.knowledge.infrastructure.KnowledgeDocumentRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeService {

    private static final int MAX_RETRIEVAL_LIMIT = 50;

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;

    public KnowledgeService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkRepository chunkRepository
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public KnowledgeDocument createDocument(
            CreateKnowledgeDocumentCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");

        String checksum = normalizeChecksum(command.checksum());
        if (documentRepository.existsByChecksum(checksum)) {
            throw new EntityExistsException(
                    "A knowledge document with this checksum already exists"
            );
        }

        KnowledgeDocument document = new KnowledgeDocument(
                normalizeRequired(command.title(), "title", 500),
                normalizeRequired(
                        command.sourceInstitution(),
                        "sourceInstitution",
                        255
                ),
                normalizeOptional(command.sourceUrl(), "sourceUrl", null),
                command.publishedAt(),
                normalizeOptional(command.version(), "version", 100),
                normalizeRequired(
                        command.originalFilename(),
                        "originalFilename",
                        500
                ),
                checksum
        );

        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public KnowledgeDocument getDocument(UUID documentId) {
        return requireDocument(documentId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> getActiveDocuments() {
        return documentRepository.findAllByStatusOrderByCreatedAtDesc(
                KnowledgeDocumentStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunk> getDocumentChunks(UUID documentId) {
        requireDocument(documentId);
        return chunkRepository.findAllByDocument_IdOrderByChunkIndex(documentId);
    }

    @Transactional
    public List<KnowledgeChunk> replaceChunksAndActivate(
            UUID documentId,
            List<EmbeddedKnowledgeChunkDraft> chunkDrafts
    ) {
        KnowledgeDocument document = requireDocument(documentId);
        requireProcessing(document);

        List<EmbeddedKnowledgeChunkDraft> validatedDrafts =
                validateAndOrderChunks(chunkDrafts);

        chunkRepository.deleteAllByDocumentId(documentId);

        List<KnowledgeChunk> chunks = validatedDrafts.stream()
                .map(draft -> toEntity(document, draft))
                .toList();
        List<KnowledgeChunk> savedChunks = chunkRepository.saveAll(chunks);

        document.markActive();
        return List.copyOf(savedChunks);
    }

    @Transactional
    public void markDocumentFailed(UUID documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        document.markFailed();
    }

    @Transactional
    public void archiveDocument(UUID documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        document.archive();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunk> findRelevantChunks(
            List<Double> queryEmbedding,
            int limit
    ) {
        float[] embedding = validateAndConvertEmbedding(queryEmbedding);
        if (limit <= 0 || limit > MAX_RETRIEVAL_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + MAX_RETRIEVAL_LIMIT
            );
        }

        return chunkRepository.findNearestByCosineDistance(
                embedding,
                KnowledgeDocumentStatus.ACTIVE,
                PageRequest.of(0, limit)
        );
    }

    private KnowledgeDocument requireDocument(UUID documentId) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Knowledge document not found"
                ));
    }

    private void requireProcessing(KnowledgeDocument document) {
        if (document.getStatus() != KnowledgeDocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Chunks can only be stored for a document being processed"
            );
        }
    }

    private List<EmbeddedKnowledgeChunkDraft> validateAndOrderChunks(
            List<EmbeddedKnowledgeChunkDraft> chunkDrafts
    ) {
        Objects.requireNonNull(chunkDrafts, "chunkDrafts must not be null");
        if (chunkDrafts.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one knowledge chunk is required"
            );
        }

        if (chunkDrafts.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("chunk draft must not be null");
        }

        List<EmbeddedKnowledgeChunkDraft> ordered = new ArrayList<>(chunkDrafts);
        ordered.sort(Comparator.comparingInt(draft -> draft.chunk().chunkIndex()));

        Set<Integer> indexes = new HashSet<>();
        for (int expectedIndex = 0; expectedIndex < ordered.size(); expectedIndex++) {
            EmbeddedKnowledgeChunkDraft draft = ordered.get(expectedIndex);
            KnowledgeChunkDraft chunk = draft.chunk();

            if (!indexes.add(chunk.chunkIndex())
                    || chunk.chunkIndex() != expectedIndex) {
                throw new IllegalArgumentException(
                        "Chunk indexes must be unique and contiguous from zero"
                );
            }
            if (chunk.content() == null || chunk.content().isBlank()) {
                throw new IllegalArgumentException(
                        "Chunk content must not be blank"
                );
            }
            if (chunk.pageFrom() <= 0 || chunk.pageTo() < chunk.pageFrom()) {
                throw new IllegalArgumentException("Invalid chunk page range");
            }
            normalizeOptional(draft.sectionTitle(), "sectionTitle", 500);
            validateAndConvertEmbedding(draft.embedding());
        }

        return ordered;
    }

    private KnowledgeChunk toEntity(
            KnowledgeDocument document,
            EmbeddedKnowledgeChunkDraft draft
    ) {
        KnowledgeChunkDraft chunk = draft.chunk();
        return new KnowledgeChunk(
                document,
                chunk.chunkIndex(),
                chunk.content().strip(),
                chunk.pageFrom(),
                chunk.pageTo(),
                normalizeOptional(
                        draft.sectionTitle(),
                        "sectionTitle",
                        500
                ),
                validateAndConvertEmbedding(draft.embedding())
        );
    }

    private float[] validateAndConvertEmbedding(List<Double> values) {
        Objects.requireNonNull(values, "embedding must not be null");
        if (values.size() != KnowledgeChunk.EMBEDDING_DIMENSIONS) {
            throw new IllegalArgumentException(
                    "Embedding must contain exactly "
                            + KnowledgeChunk.EMBEDDING_DIMENSIONS
                            + " values"
            );
        }

        float[] embedding = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            Double value = values.get(index);
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException(
                        "Embedding values must be finite numbers"
                );
            }
            embedding[index] = value.floatValue();
        }
        return embedding;
    }

    private String normalizeChecksum(String value) {
        String checksum = normalizeRequired(value, "checksum", 64)
                .toLowerCase(Locale.ROOT);
        if (!checksum.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "checksum must be a SHA-256 value encoded as 64 hexadecimal characters"
            );
        }
        return checksum;
    }

    private String normalizeRequired(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maxLength + " characters"
            );
        }
        return normalized;
    }

    private String normalizeOptional(
            String value,
            String field,
            Integer maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (maxLength != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maxLength + " characters"
            );
        }
        return normalized;
    }
}
