package ro.scribemed.backend.shared.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ro.scribemed.backend.consultation.application.exception.ConsultationNotFoundException;

class ApiExceptionHandlerTests {

    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    @Test
    void mapsConsultationNotFoundToSafe404Response() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler
                .handleConsultationResourceNotFound(new ConsultationNotFoundException());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
                "CONSULTATION_NOT_FOUND",
                "Consultația nu a fost găsită."
        ));
    }

    @Test
    void mapsInvalidConsultationStateToConflict() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleConsultationStateException();

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("PATIENT_INFORMATION_REQUIRED");
    }

    @Test
    void mapsOversizedAudioToPayloadTooLarge() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAudioFileTooLargeException();

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().code()).isEqualTo("AUDIO_FILE_TOO_LARGE");
    }

    @Test
    void mapsUnsupportedAudioTypeToUnsupportedMediaType() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUnsupportedAudioTypeException();

        assertThat(response.getStatusCode().value()).isEqualTo(415);
        assertThat(response.getBody().code()).isEqualTo("UNSUPPORTED_AUDIO_TYPE");
    }

    @Test
    void mapsAudioStorageFailureToSanitizedServiceUnavailableResponse() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler
                .handleAudioStorageUnavailableException();

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
                "AUDIO_STORAGE_UNAVAILABLE",
                "Fișierul audio nu a putut fi stocat. Încercați din nou."
        ));
    }
}
