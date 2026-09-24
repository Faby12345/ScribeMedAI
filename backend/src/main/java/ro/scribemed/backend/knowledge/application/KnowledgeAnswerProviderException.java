package ro.scribemed.backend.knowledge.application;

public class KnowledgeAnswerProviderException extends RuntimeException {

    private final String safeErrorCode;

    public KnowledgeAnswerProviderException(String safeErrorCode, String message) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() {
        return safeErrorCode;
    }
}
