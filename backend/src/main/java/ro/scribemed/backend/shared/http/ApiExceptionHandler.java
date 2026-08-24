package ro.scribemed.backend.shared.http;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ro.scribemed.backend.document.application.DocumentStateException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> handleIllegalArgumentException() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<Void> handleEntityNotFoundException() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Void> handleAccessDeniedException() {
        return ResponseEntity.status(403).build();
    }

    @ExceptionHandler(DocumentStateException.class)
    ResponseEntity<Void> handleDocumentStateException() {
        return ResponseEntity.status(409).build();
    }
}
