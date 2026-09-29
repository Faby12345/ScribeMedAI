package ro.scribemed.backend.knowledge.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.knowledge.domain.KnowledgeChunk;
import ro.scribemed.backend.knowledge.dto.KnowledgeQueryResponse;
import ro.scribemed.backend.knowledge.dto.SearchQueryRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class KnowledgeQueryService {
    private static final int RETRIEVAL_LIMIT = 6;

    private final EmbeddingProvider embeddingProvider;
    private final KnowledgeService knowledgeService;
    private final KnowledgeAnswerProvider answerProvider;

    public KnowledgeQueryService(
            EmbeddingProvider embeddingProvider,
            KnowledgeService knowledgeService,
            KnowledgeAnswerProvider answerProvider
    ) {
        this.embeddingProvider = embeddingProvider;
        this.knowledgeService = knowledgeService;
        this.answerProvider = answerProvider;
    }

    @Transactional(readOnly = true)
    public KnowledgeQueryResponse query(
            UUID tenantId,
            SearchQueryRequest request
    ) {
        String question = request.query().trim();
        List<Double> queryEmbedding = embeddingProvider.embed(question);

        List<KnowledgeChunk> chunks = knowledgeService.findRelevantChunks(
                tenantId,
                request.documentsIds(),
                queryEmbedding,
                RETRIEVAL_LIMIT
        );

        if (chunks.isEmpty()) {
            return new KnowledgeQueryResponse(
                    "Nu am găsit informații relevante în documentele selectate.",
                    List.of()
            );
        }


        List<KnowledgeAnswerProvider.KnowledgeContext> contexts =
                IntStream.range(0, chunks.size())
                        .mapToObj(index -> {
                            KnowledgeChunk chunk = chunks.get(index);

                            return new
                                    KnowledgeAnswerProvider.KnowledgeContext(
                                    index + 1,
                                    chunk.getDocument().getTitle(),
                                    chunk.getPageFrom(),
                                    chunk.getPageTo(),
                                    chunk.getSectionTitle(),
                                    chunk.getContent()
                            );
                        })
                        .toList();

        KnowledgeAnswerProvider.KnowledgeAnswer generated =
                answerProvider.generateAnswer(question, contexts);

        List<KnowledgeQueryResponse.KnowledgeCitationResponse> citations =
                chunks.stream()
                        .map(chunk -> new
                                KnowledgeQueryResponse.KnowledgeCitationResponse(
                                chunk.getDocument().getId(),
                                chunk.getDocument().getTitle(),
                                chunk.getPageFrom(),
                                chunk.getPageTo(),
                                chunk.getSectionTitle(),
                                safeExcerpt(chunk.getContent())
                        ))
                        .toList();

        return new KnowledgeQueryResponse(generated.answer(), citations);

    }

    private String safeExcerpt(String content) {
        int maximumLength = 500;
        return content.length() <= maximumLength
                ? content
                : content.substring(0, maximumLength) + "…";
    }
}
