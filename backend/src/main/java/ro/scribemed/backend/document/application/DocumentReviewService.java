package ro.scribemed.backend.document.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.ClinicalDocumentStatus;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.domain.DocumentVersionSource;
import ro.scribemed.backend.document.domain.DocumentVersionStatus;
import ro.scribemed.backend.document.dto.ApproveDocumentRequest;
import ro.scribemed.backend.document.dto.DocumentApprovalResponse;
import ro.scribemed.backend.document.dto.DocumentReviewResponse;
import ro.scribemed.backend.document.dto.SaveDocumentDraftRequest;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.document.infrastructure.DocumentVersionRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.transcription.domain.ConsultationTranscript;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

@Service
public class DocumentReviewService {

    private static final String SOAP_NOTE_DOCUMENT_TYPE = "SOAP_NOTE";
    private static final TypeReference<List<String>> REVIEW_FLAGS_TYPE = new TypeReference<>() {
    };

    private final ClinicalDocumentRepository clinicalDocumentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ConsultationTranscriptRepository transcriptRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public DocumentReviewService(
            ClinicalDocumentRepository clinicalDocumentRepository,
            DocumentVersionRepository documentVersionRepository,
            ConsultationTranscriptRepository transcriptRepository,
            AppUserRepository appUserRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.transcriptRepository = transcriptRepository;
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DocumentReviewResponse getConsultationReviewDocument(UUID consultationId, UUID tenantId) {
        ClinicalDocument document = clinicalDocumentRepository
                .findByConsultation_IdAndTenant_IdAndDocumentType(
                        consultationId,
                        tenantId,
                        SOAP_NOTE_DOCUMENT_TYPE
                )
                .orElseThrow(() -> new EntityNotFoundException("Clinical document not found"));

        DocumentVersion version = getReviewVersion(document, tenantId);

        ConsultationTranscript transcript = transcriptRepository
                .findByConsultation_IdAndTenant_Id(consultationId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Transcript not found"));

        return toReviewResponse(consultationId, document, version, transcript);
    }

    @Transactional
    public DocumentReviewResponse saveDraft(
            UUID documentId,
            UUID tenantId,
            UUID userId,
            SaveDocumentDraftRequest request
    ) {
        ClinicalDocument document = getEditableDocument(documentId, tenantId);
        AppUser actorUser = getTenantUser(userId, tenantId);

        DocumentVersion latestDraft = documentVersionRepository
                .findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                        documentId,
                        tenantId,
                        DocumentVersionStatus.DRAFT
                )
                .orElseThrow(() -> new EntityNotFoundException("Draft document version not found"));

        documentVersionRepository.findByDocument_IdAndTenant_IdOrderByVersionNumberDesc(documentId, tenantId)
                .stream()
                .filter(version -> version.getStatus() == DocumentVersionStatus.DRAFT)
                .forEach(DocumentVersion::markSuperseded);

        int versionNumber = document.nextVersionNumber();

        DocumentVersion savedVersion = documentVersionRepository.save(new DocumentVersion(
                document.getTenant(),
                document,
                document.getConsultation(),
                latestDraft.getSourceNotes(),
                versionNumber,
                DocumentVersionSource.DOCTOR_EDITED,
                normalizeText(request.draft().subjective()),
                normalizeText(request.draft().objective()),
                normalizeText(request.draft().assessment()),
                normalizeText(request.draft().plan()),
                writeReviewFlags(request.reviewFlags(), latestDraft.getReviewFlags()),
                latestDraft.getAiProvider(),
                latestDraft.getAiModel(),
                latestDraft.getPromptVersion(),
                latestDraft.getTemplateVersion(),
                actorUser
        ));

        auditService.record(
                document.getTenant(),
                actorUser,
                "DOCUMENT_DRAFT_SAVED",
                "CLINICAL_DOCUMENT",
                document.getId(),
                Map.of(
                        "versionNumber", savedVersion.getVersionNumber(),
                        "source", savedVersion.getSource().name()
                )
        );

        ConsultationTranscript transcript = transcriptRepository
                .findByConsultation_IdAndTenant_Id(document.getConsultation().getId(), tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Transcript not found"));

        return toReviewResponse(document.getConsultation().getId(), document, savedVersion, transcript);
    }

    @Transactional
    public DocumentApprovalResponse approveDocument(
            UUID documentId,
            UUID tenantId,
            UUID userId,
            ApproveDocumentRequest request
    ) {
        if (!request.doctorReviewedAndApproved()) {
            throw new IllegalArgumentException("Doctor review confirmation is required");
        }

        ClinicalDocument document = getEditableDocument(documentId, tenantId);
        AppUser approvingUser = getTenantUser(userId, tenantId);

        DocumentVersion draftVersion = documentVersionRepository
                .findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                        documentId,
                        tenantId,
                        DocumentVersionStatus.DRAFT
                )
                .orElseThrow(() -> new EntityNotFoundException("Draft document version not found"));

        if (!request.versionId().equals(draftVersion.getId())) {
            throw new DocumentStateException("Draft version is no longer current");
        }

        Instant approvedAt = Instant.now();
        draftVersion.markApproved(approvingUser, approvedAt);
        document.markApproved(approvingUser, approvedAt);

        auditService.record(
                document.getTenant(),
                approvingUser,
                "DOCUMENT_APPROVED",
                "CLINICAL_DOCUMENT",
                document.getId(),
                Map.of("versionNumber", draftVersion.getVersionNumber())
        );

        return new DocumentApprovalResponse(
                document.getId(),
                document.getStatus(),
                draftVersion.getId(),
                draftVersion.getVersionNumber(),
                draftVersion.getStatus(),
                draftVersion.getApprovedAt()
        );
    }

    private ClinicalDocument getEditableDocument(UUID documentId, UUID tenantId) {
        ClinicalDocument document = clinicalDocumentRepository.findByIdAndTenant_Id(documentId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Clinical document not found"));

        if (document.getStatus() != ClinicalDocumentStatus.DRAFT) {
            throw new DocumentStateException("Clinical document is not editable");
        }

        return document;
    }

    private AppUser getTenantUser(UUID userId, UUID tenantId) {
        return appUserRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private DocumentVersion getReviewVersion(ClinicalDocument document, UUID tenantId) {
        DocumentVersionStatus versionStatus = document.getStatus() == ClinicalDocumentStatus.APPROVED
                ? DocumentVersionStatus.APPROVED
                : DocumentVersionStatus.DRAFT;

        return documentVersionRepository
                .findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                        document.getId(),
                        tenantId,
                        versionStatus
                )
                .orElseThrow(() -> new EntityNotFoundException("Document version not found"));
    }

    private List<String> parseReviewFlags(String reviewFlagsJson) {
        if (reviewFlagsJson == null || reviewFlagsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(reviewFlagsJson, REVIEW_FLAGS_TYPE);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Review flags could not be parsed", error);
        }
    }

    private String writeReviewFlags(List<String> reviewFlags, String fallbackJson) {
        if (reviewFlags == null) {
            return fallbackJson == null || fallbackJson.isBlank() ? "[]" : fallbackJson;
        }

        try {
            return objectMapper.writeValueAsString(reviewFlags);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Review flags could not be serialized", error);
        }
    }

    private String normalizeText(String value) {
        return value == null ? "" : value;
    }

    private DocumentReviewResponse toReviewResponse(
            UUID consultationId,
            ClinicalDocument document,
            DocumentVersion version,
            ConsultationTranscript transcript
    ) {
        return new DocumentReviewResponse(
                consultationId,
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                version.getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getSource(),
                new DocumentReviewResponse.SoapDraftResponse(
                        version.getSubjective(),
                        version.getObjective(),
                        version.getAssessment(),
                        version.getPlan()
                ),
                parseReviewFlags(version.getReviewFlags()),
                version.getAiProvider(),
                version.getAiModel(),
                version.getPromptVersion(),
                version.getTemplateVersion(),
                version.getCreatedAt(),
                new DocumentReviewResponse.TranscriptForReviewResponse(
                        transcript.getProvider(),
                        transcript.getProviderModel(),
                        transcript.getLanguage(),
                        transcript.getTranscriptText(),
                        transcript.getCreatedAt()
                )
        );
    }
}
