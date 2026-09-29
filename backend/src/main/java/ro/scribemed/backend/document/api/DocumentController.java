package ro.scribemed.backend.document.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ro.scribemed.backend.document.application.DocumentHistoryService;
import ro.scribemed.backend.document.application.DocumentReviewService;
import ro.scribemed.backend.document.dto.ApproveDocumentRequest;
import ro.scribemed.backend.document.dto.DocumentApprovalResponse;
import ro.scribemed.backend.document.dto.DocumentReviewResponse;
import ro.scribemed.backend.document.dto.PatientDocumentSummaryResponse;
import ro.scribemed.backend.document.dto.SaveDocumentDraftRequest;
import ro.scribemed.backend.identity.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentReviewService documentReviewService;
    private final DocumentHistoryService documentHistoryService;

    public DocumentController(
            DocumentReviewService documentReviewService,
            DocumentHistoryService documentHistoryService
    ) {
        this.documentReviewService = documentReviewService;
        this.documentHistoryService = documentHistoryService;
    }

    @GetMapping("/consultations/{consultationId}/document")
    ResponseEntity<DocumentReviewResponse> getConsultationDocument(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID consultationId
    ) {
        DocumentReviewResponse response = documentReviewService.getConsultationReviewDocument(
                consultationId,
                currentUser.tenantId()
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/documents/{documentId}/draft")
    ResponseEntity<DocumentReviewResponse> saveDraft(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID documentId,
            @Valid @RequestBody SaveDocumentDraftRequest request
    ) {
        DocumentReviewResponse response = documentReviewService.saveDraft(
                documentId,
                currentUser.tenantId(),
                currentUser.userId(),
                request
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/documents/{documentId}/approval")
    ResponseEntity<DocumentApprovalResponse> approveDocument(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID documentId,
            @Valid @RequestBody ApproveDocumentRequest request
    ) {
        DocumentApprovalResponse response = documentReviewService.approveDocument(
                documentId,
                currentUser.tenantId(),
                currentUser.userId(),
                request
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/documents")
    ResponseEntity<List<PatientDocumentSummaryResponse>> getPatientDocuments(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID patientId
    ) {
        List<PatientDocumentSummaryResponse> response = documentHistoryService.getPatientDocuments(
                patientId,
                currentUser.tenantId()
        );
        return ResponseEntity.ok(response);
    }
}
