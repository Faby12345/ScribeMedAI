package ro.scribemed.backend.mockPias.domain;


/**
 * THIS IS A MOCK DTO FOR RESULT - FOR A MOCK SERVER NOT THE
 * REAL PIAS SERVER
 * */
public record PiasValidationResult(
        PiasValidationStatus status,
        String responseCode,
        String safeMessage
) {

    public boolean isValid() {
        return status == PiasValidationStatus.VALID;
    }
}