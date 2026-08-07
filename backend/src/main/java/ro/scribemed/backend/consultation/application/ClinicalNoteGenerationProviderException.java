package ro.scribemed.backend.consultation.application;

public class ClinicalNoteGenerationProviderException extends RuntimeException {

    private final String safeErrorCode;

    public ClinicalNoteGenerationProviderException(String safeErrorCode, String message) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() {
        return safeErrorCode;
    }
}
