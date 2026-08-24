package ro.scribemed.backend.document.application;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.domain.DocumentVersionStatus;
import ro.scribemed.backend.document.dto.DocumentReviewResponse;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.document.infrastructure.DocumentVersionRepository;
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
    private final ObjectMapper objectMapper;

    public DocumentReviewService(
            ClinicalDocumentRepository clinicalDocumentRepository,
            DocumentVersionRepository documentVersionRepository,
            ConsultationTranscriptRepository transcriptRepository,
            ObjectMapper objectMapper
    ) {
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.transcriptRepository = transcriptRepository;
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

        DocumentVersion version = documentVersionRepository
                .findFirstByDocument_IdAndTenant_IdAndStatusOrderByVersionNumberDesc(
                        document.getId(),
                        tenantId,
                        DocumentVersionStatus.DRAFT
                )
                .orElseThrow(() -> new EntityNotFoundException("Draft document version not found"));

        ConsultationTranscript transcript = transcriptRepository
                .findByConsultation_IdAndTenant_Id(consultationId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Transcript not found"));

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
}
