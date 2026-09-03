package ro.scribemed.backend.document.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ro.scribemed.backend.document.application.DocumentHistoryService;
import ro.scribemed.backend.document.application.DocumentReviewService;
import ro.scribemed.backend.document.dto.ApproveDocumentRequest;
import ro.scribemed.backend.document.dto.DocumentApprovalResponse;
import ro.scribemed.backend.document.dto.DocumentReviewResponse;
import ro.scribemed.backend.document.dto.PatientDocumentSummaryResponse;
import ro.scribemed.backend.document.dto.SaveDocumentDraftRequest;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.security.CurrentUser;

class DocumentControllerTests {

    private final DocumentReviewService documentReviewService = mock(DocumentReviewService.class);
    private final DocumentHistoryService documentHistoryService = mock(DocumentHistoryService.class);
    private final DocumentController controller = new DocumentController(
            documentReviewService,
            documentHistoryService
    );

    @Test
    void returnsOkForConsultationDocument() {
        CurrentUser currentUser = currentUser();
        UUID consultationId = UUID.randomUUID();
        DocumentReviewResponse document = mock(DocumentReviewResponse.class);
        when(documentReviewService.getConsultationReviewDocument(consultationId, currentUser.tenantId()))
                .thenReturn(document);

        ResponseEntity<DocumentReviewResponse> response = controller
                .getConsultationDocument(currentUser, consultationId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(document);
    }

    @Test
    void returnsOkForSavedDraft() {
        CurrentUser currentUser = currentUser();
        UUID documentId = UUID.randomUUID();
        SaveDocumentDraftRequest request = new SaveDocumentDraftRequest(
                new SaveDocumentDraftRequest.SoapDraftRequest(null, null, null, null),
                List.of()
        );
        DocumentReviewResponse document = mock(DocumentReviewResponse.class);
        when(documentReviewService.saveDraft(documentId, currentUser.tenantId(), currentUser.userId(), request))
                .thenReturn(document);

        ResponseEntity<DocumentReviewResponse> response = controller.saveDraft(currentUser, documentId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(document);
    }

    @Test
    void returnsOkForDocumentApproval() {
        CurrentUser currentUser = currentUser();
        UUID documentId = UUID.randomUUID();
        ApproveDocumentRequest request = new ApproveDocumentRequest(UUID.randomUUID(), true);
        DocumentApprovalResponse approval = mock(DocumentApprovalResponse.class);
        when(documentReviewService.approveDocument(documentId, currentUser.tenantId(), currentUser.userId(), request))
                .thenReturn(approval);

        ResponseEntity<DocumentApprovalResponse> response = controller
                .approveDocument(currentUser, documentId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(approval);
    }

    @Test
    void returnsOkForPatientDocuments() {
        CurrentUser currentUser = currentUser();
        UUID patientId = UUID.randomUUID();
        List<PatientDocumentSummaryResponse> documents = List.of(mock(PatientDocumentSummaryResponse.class));
        when(documentHistoryService.getPatientDocuments(patientId, currentUser.tenantId()))
                .thenReturn(documents);

        ResponseEntity<List<PatientDocumentSummaryResponse>> response = controller
                .getPatientDocuments(currentUser, patientId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(documents);
        verify(documentHistoryService).getPatientDocuments(patientId, currentUser.tenantId());
    }

    private CurrentUser currentUser() {
        return new CurrentUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "doctor@example.com",
                "Dr. Test",
                UserRole.DOCTOR,
                UUID.randomUUID()
        );
    }
}
