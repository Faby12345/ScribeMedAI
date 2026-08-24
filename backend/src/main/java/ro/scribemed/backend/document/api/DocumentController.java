package ro.scribemed.backend.document.api;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.scribemed.backend.document.application.DocumentReviewService;
import ro.scribemed.backend.document.dto.DocumentReviewResponse;
import ro.scribemed.backend.identity.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentReviewService documentReviewService;

    public DocumentController(DocumentReviewService documentReviewService) {
        this.documentReviewService = documentReviewService;
    }

    @GetMapping("/consultations/{consultationId}/document")
    DocumentReviewResponse getConsultationDocument(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID consultationId
    ) {
        return documentReviewService.getConsultationReviewDocument(
                consultationId,
                currentUser.tenantId()
        );
    }
}
