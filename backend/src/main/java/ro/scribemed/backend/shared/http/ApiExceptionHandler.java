package ro.scribemed.backend.shared.http;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ro.scribemed.backend.consultation.application.exception.AudioFileRequiredException;
import ro.scribemed.backend.consultation.application.exception.AudioFileTooLargeException;
import ro.scribemed.backend.consultation.application.exception.AudioStorageUnavailableException;
import ro.scribemed.backend.consultation.application.exception.ConsultationNotFoundException;
import ro.scribemed.backend.consultation.application.exception.ConsultationStateException;
import ro.scribemed.backend.consultation.application.exception.PatientNotFoundException;
import ro.scribemed.backend.consultation.application.exception.TenantNotFoundException;
import ro.scribemed.backend.consultation.application.exception.TranscriptNotAvailableException;
import ro.scribemed.backend.consultation.application.exception.UnsupportedAudioTypeException;
import ro.scribemed.backend.document.application.DocumentStateException;
import ro.scribemed.backend.identity.application.AuthenticationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgumentException() {
        return badRequest("INVALID_REQUEST", "Cererea este invalidă.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleEntityNotFoundException() {
        return ResponseEntity.status(404)
                .body(new ApiErrorResponse("RESOURCE_NOT_FOUND", "Resursa nu a fost găsită."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDeniedException() {
        return ResponseEntity.status(403)
                .body(new ApiErrorResponse("ACCESS_DENIED", "Nu aveți permisiunea pentru această acțiune."));
    }

    @ExceptionHandler(DocumentStateException.class)
    ResponseEntity<ApiErrorResponse> handleDocumentStateException() {
        return conflict("DOCUMENT_STATE_INVALID", "Documentul nu este într-o stare validă pentru această acțiune.");
    }

    @ExceptionHandler({ConsultationNotFoundException.class, PatientNotFoundException.class, TenantNotFoundException.class})
    ResponseEntity<ApiErrorResponse> handleConsultationResourceNotFound(RuntimeException exception) {
        if (exception instanceof ConsultationNotFoundException) {
            return ResponseEntity.status(404)
                    .body(new ApiErrorResponse("CONSULTATION_NOT_FOUND", "Consultația nu a fost găsită."));
        }
        if (exception instanceof PatientNotFoundException) {
            return ResponseEntity.status(404)
                    .body(new ApiErrorResponse("PATIENT_NOT_FOUND", "Pacientul nu a fost găsit."));
        }
        return ResponseEntity.status(404)
                .body(new ApiErrorResponse("TENANT_NOT_FOUND", "Clinica nu a fost găsită."));
    }

    @ExceptionHandler(TranscriptNotAvailableException.class)
    ResponseEntity<ApiErrorResponse> handleTranscriptNotAvailableException() {
        return conflict("TRANSCRIPT_NOT_AVAILABLE", "Transcrierea nu este disponibilă încă.");
    }

    @ExceptionHandler(ConsultationStateException.class)
    ResponseEntity<ApiErrorResponse> handleConsultationStateException() {
        return conflict(
                "PATIENT_INFORMATION_REQUIRED",
                "Confirmarea informării pacientului este necesară înainte de încărcarea audio."
        );
    }

    @ExceptionHandler(AudioFileRequiredException.class)
    ResponseEntity<ApiErrorResponse> handleAudioFileRequiredException() {
        return badRequest("AUDIO_FILE_REQUIRED", "Fișierul audio este obligatoriu.");
    }

    @ExceptionHandler(AudioFileTooLargeException.class)
    ResponseEntity<ApiErrorResponse> handleAudioFileTooLargeException() {
        return ResponseEntity.status(413)
                .body(new ApiErrorResponse("AUDIO_FILE_TOO_LARGE", "Fișierul audio depășește dimensiunea maximă permisă."));
    }

    @ExceptionHandler(UnsupportedAudioTypeException.class)
    ResponseEntity<ApiErrorResponse> handleUnsupportedAudioTypeException() {
        return ResponseEntity.status(415)
                .body(new ApiErrorResponse("UNSUPPORTED_AUDIO_TYPE", "Tipul fișierului audio nu este acceptat."));
    }

    @ExceptionHandler(AudioStorageUnavailableException.class)
    ResponseEntity<ApiErrorResponse> handleAudioStorageUnavailableException() {
        return ResponseEntity.status(503)
                .body(new ApiErrorResponse("AUDIO_STORAGE_UNAVAILABLE", "Fișierul audio nu a putut fi stocat. Încercați din nou."));
    }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthenticationException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        "AUTHENTICATION_FAILED",
                        "Emailul sau parola sunt incorecte."
                ));
    }


    private ResponseEntity<ApiErrorResponse> badRequest(String code, String message) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(code, message));
    }

    private ResponseEntity<ApiErrorResponse> conflict(String code, String message) {
        return ResponseEntity.status(409).body(new ApiErrorResponse(code, message));
    }
}
