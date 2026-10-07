package ro.scribemed.backend.consultation.application;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.scribemed.backend.audio.application.AudioStorageService;
import ro.scribemed.backend.audio.application.StoredAudio;
import ro.scribemed.backend.audio.domain.ConsultationAudio;
import ro.scribemed.backend.audio.infrastructure.ConsultationAudioRepository;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.consultation.application.exception.AudioFileRequiredException;
import ro.scribemed.backend.consultation.application.exception.AudioFileTooLargeException;
import ro.scribemed.backend.consultation.application.exception.AudioStorageUnavailableException;
import ro.scribemed.backend.consultation.application.exception.ConsultationNotFoundException;
import ro.scribemed.backend.consultation.application.exception.ConsultationStateException;
import ro.scribemed.backend.consultation.application.exception.PatientNotFoundException;
import ro.scribemed.backend.consultation.application.exception.TenantNotFoundException;
import ro.scribemed.backend.consultation.application.exception.TranscriptNotAvailableException;
import ro.scribemed.backend.consultation.application.exception.UnsupportedAudioTypeException;
import ro.scribemed.backend.consultation.dto.AudioUploadResponse;
import ro.scribemed.backend.consultation.dto.ConsultationResponse;
import ro.scribemed.backend.consultation.dto.CreateConsultationRequest;
import ro.scribemed.backend.consultation.dto.NotesRequest;
import ro.scribemed.backend.consultation.dto.NotesResponse;
import ro.scribemed.backend.consultation.dto.TranscriptResponse;
import ro.scribemed.backend.consultation.infrastructure.ConsultationNotesRepository;
import ro.scribemed.backend.consultation.infrastructure.ConsultationRepository;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.domain.DocumentVersion;
import ro.scribemed.backend.document.domain.DocumentVersionSource;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.document.infrastructure.DocumentVersionRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.infrastructure.PatientRepository;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobType;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;
import ro.scribemed.backend.prescribedMedication.application.PrescribedMedicationService;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

@Service
public class ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/webm",
            "audio/wav",
            "audio/x-wav",
            "audio/mpeg",
            "audio/mp4",
            "audio/ogg",
            "audio/x-m4a",
            "audio/m4a",
            "audio/webm;codecs=opus"
    );

    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final ConsultationAudioRepository audioRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final ConsultationTranscriptRepository transcriptRepository;
    private final AudioStorageService audioStorageService;
    private final AuditService auditService;
    private final long maxAudioSizeBytes;
    private final ConsultationNotesRepository consultationNotesRepository;
    private final PrescribedMedicationService prescribedMedicationService;
    private final ClinicalDocumentRepository clinicalDocumentRepository;
    private final DocumentVersionRepository documentVersionRepository;

    public ConsultationService(
            ConsultationRepository consultationRepository,
            PatientRepository patientRepository,
            AppUserRepository appUserRepository,
            TenantRepository tenantRepository,
            ConsultationAudioRepository audioRepository,
            ProcessingJobRepository processingJobRepository,
            ConsultationTranscriptRepository transcriptRepository,
            AudioStorageService audioStorageService,
            AuditService auditService,
            @Value("${scribemed.audio.max-size-bytes}") long maxAudioSizeBytes,
            ConsultationNotesRepository consultationNotesRepository,
            PrescribedMedicationService prescribedMedicationService,
            ClinicalDocumentRepository clinicalDocumentRepository,
            DocumentVersionRepository documentVersionRepository
    ) {
        this.consultationRepository = consultationRepository;
        this.patientRepository = patientRepository;
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
        this.audioRepository = audioRepository;
        this.processingJobRepository = processingJobRepository;
        this.transcriptRepository = transcriptRepository;
        this.audioStorageService = audioStorageService;
        this.auditService = auditService;
        this.maxAudioSizeBytes = maxAudioSizeBytes;
        this.consultationNotesRepository = consultationNotesRepository;
        this.prescribedMedicationService = prescribedMedicationService;
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.documentVersionRepository = documentVersionRepository;
    }

    @Transactional
    public ConsultationResponse createConsultation(CreateConsultationRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(TenantNotFoundException::new);
        Patient patient = patientRepository.findByIdAndTenant_Id(request.patientId(), request.tenantId())
                .orElseThrow(PatientNotFoundException::new);
        AppUser actorUser = appUserRepository.findByIdAndTenant_Id(request.actorUserId(), request.tenantId())
                .orElseThrow(() -> new AccessDeniedException("Actor user is not part of the tenant"));

        Consultation consultation = consultationRepository.save(new Consultation(tenant, patient, actorUser));

        auditService.record(
                tenant,
                actorUser,
                "CONSULTATION_CREATED",
                "consultation",
                consultation.getId(),
                Map.of("status", "created")
        );

        return ConsultationResponse.from(consultation);
    }

    @Transactional
    public ConsultationResponse confirmPatientInformed(UUID consultationId, UUID tenantId, UUID actorUserId) {
        Consultation consultation = getTenantConsultation(consultationId, tenantId);
        AppUser actorUser = appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Actor user is not part of the tenant"));

        consultation.markPatientInformed(Instant.now());
        auditService.record(
                consultation.getTenant(),
                actorUser,
                "CONSULTATION_PATIENT_INFORMED",
                "consultation",
                consultation.getId(),
                Map.of("status", "confirmed")
        );

        return ConsultationResponse.from(consultation);
    }

    @Transactional
    public AudioUploadResponse uploadAudio(
            UUID consultationId,
            UUID tenantId,
            UUID actorUserId,
            MultipartFile file
    ) {
        Consultation consultation = getTenantConsultation(consultationId, tenantId);
        AppUser actorUser = appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Actor user is not part of the tenant"));

        if (consultation.getPatientInformedAt() == null) {
            throw new ConsultationStateException();
        }

        validateAudio(file);

        String objectKey = buildObjectKey(tenantId, consultationId, file.getOriginalFilename());
        StoredAudio storedAudio;
        try {
            storedAudio = audioStorageService.store(objectKey, file.getInputStream(), file.getSize());
        } catch (IOException error) {
            log.warn("Audio storage failed tenantId={} consultationId={}", tenantId, consultationId);
            throw new AudioStorageUnavailableException(error);
        }

        ConsultationAudio audio = audioRepository.save(new ConsultationAudio(
                consultation.getTenant(),
                consultation,
                storedAudio.objectKey(),
                null,
                file.getContentType(),
                storedAudio.sizeBytes(),
                storedAudio.checksumSha256()
        ));
        ProcessingJob job = processingJobRepository.save(new ProcessingJob(
                consultation.getTenant(),
                consultation,
                ProcessingJobType.TRANSCRIPTION
        ));
        consultation.markAudioUploaded();

        auditService.record(
                consultation.getTenant(),
                actorUser,
                "CONSULTATION_AUDIO_UPLOADED",
                "consultation",
                consultation.getId(),
                Map.of("status", "uploaded")
        );

        return new AudioUploadResponse(
                consultation.getId(),
                audio.getId(),
                job.getId(),
                consultation.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultation(UUID consultationId, UUID tenantId) {
        return ConsultationResponse.from(getTenantConsultation(consultationId, tenantId));
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getAllConsultations(UUID tenantId, Pageable pageable) {
        return consultationRepository.findResponsesByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public TranscriptResponse getTranscript(UUID consultationId, UUID tenantId) {
        getTenantConsultation(consultationId, tenantId);
        return transcriptRepository.findByConsultation_IdAndTenant_Id(consultationId, tenantId)
                .map(TranscriptResponse::from)
                .orElseThrow(TranscriptNotAvailableException::new);
    }

    private Consultation getTenantConsultation(UUID consultationId, UUID tenantId) {
        return consultationRepository.findByIdAndTenant_Id(consultationId, tenantId)
                .orElseThrow(ConsultationNotFoundException::new);
    }

    private void validateAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AudioFileRequiredException();
        }
        if (file.getSize() > maxAudioSizeBytes) {
            throw new AudioFileTooLargeException();
        }
        if (file.getContentType() == null || !ALLOWED_AUDIO_TYPES.contains(file.getContentType())) {
            throw new UnsupportedAudioTypeException();
        }
    }

    private String buildObjectKey(UUID tenantId, UUID consultationId, String originalFilename) {
        return "tenant/%s/consultation/%s/%s%s".formatted(
                tenantId,
                consultationId,

                UUID.randomUUID(),
                extensionFrom(originalFilename)
        );
    }

    private String extensionFrom(String originalFilename) {
        if (originalFilename == null) {
            return ".audio";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return ".audio";
        }
        return originalFilename.substring(dotIndex).replaceAll("[^A-Za-z0-9.]", "");
    }



    @Transactional
    public NotesResponse processNotes(NotesRequest request, UUID tenantId, UUID appUserId, UUID consultationId){

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(TenantNotFoundException::new);

        AppUser appUser = appUserRepository.findByIdAndTenant_Id(appUserId, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Actor user is not part of the tenant"));

        Consultation consultation = getTenantConsultation(consultationId, tenantId);

        // add clinical note to db
        ConsultationNotes notes = ConsultationNotes.create(
                tenant,
                consultation,
                appUser,
                request.reason(),
                request.history(),
                request.objective(),
                request.assessment(),
                request.plan()
        );

        ConsultationNotes savedNotes = consultationNotesRepository.save(notes);

        prescribedMedicationService.createPlan(
                tenant,
                savedNotes,
                request.medications()
        );

        ClinicalDocument document = clinicalDocumentRepository
                .findByConsultation_IdAndTenant_IdAndDocumentType(
                        consultationId,
                        tenantId,
                        "SOAP_NOTE"
                )
                .orElseGet(() -> clinicalDocumentRepository.save(new ClinicalDocument(
                        tenant,
                        consultation,
                        "SOAP_NOTE"
                )));

        DocumentVersion version = documentVersionRepository.save(new DocumentVersion(
                tenant,
                document,
                consultation,
                savedNotes,
                document.nextVersionNumber(),
                DocumentVersionSource.DOCTOR_CREATED,
                buildSubjective(request.reason(), request.history()),
                normalizeText(request.objective()),
                normalizeText(request.assessment()),
                normalizeText(request.plan()),
                "[]",
                null,
                null,
                null,
                null,
                appUser
        ));

        consultation.markNotesReady();

        auditService.record(
                tenant,
                appUser,
                "MANUAL_CLINICAL_DOCUMENT_CREATED",
                "CLINICAL_DOCUMENT",
                document.getId(),
                Map.of(
                        "versionNumber", version.getVersionNumber(),
                        "source", version.getSource().name()
                )
        );

        return new NotesResponse(
                savedNotes.getId(),
                document.getId(),
                version.getId(),
                consultationId,
                version.getStatus()
        );
    }

    private String buildSubjective(String reason, String history) {
        String normalizedReason = normalizeText(reason);
        String normalizedHistory = normalizeText(history);

        if (normalizedReason.isEmpty()) {
            return normalizedHistory;
        }
        if (normalizedHistory.isEmpty()) {
            return normalizedReason;
        }
        return "Motivul prezentării:\n%s\n\nAnamneză și simptome:\n%s"
                .formatted(normalizedReason, normalizedHistory);
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

}
