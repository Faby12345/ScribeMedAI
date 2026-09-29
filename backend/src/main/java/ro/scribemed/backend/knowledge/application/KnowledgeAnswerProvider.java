package ro.scribemed.backend.knowledge.application;

import java.util.List;

public interface KnowledgeAnswerProvider {
    KnowledgeAnswer generateAnswer(
            String question,
            List<KnowledgeContext> contexts
    );

    record KnowledgeContext(
            int sourceNumber,
            String documentTitle,
            Integer pageFrom,
            Integer pageTo,
            String sectionTitle,
            String content
    ) {
    }

    record KnowledgeAnswer(String answer) {
    }
}
