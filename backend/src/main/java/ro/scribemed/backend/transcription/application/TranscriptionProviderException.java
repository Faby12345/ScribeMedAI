package ro.scribemed.backend.transcription.application;

public class TranscriptionProviderException extends RuntimeException {

    private final String safeErrorCode;

    public TranscriptionProviderException(String safeErrorCode, String message) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() {
        return safeErrorCode;
    }
}
