package ro.scribemed.backend.consultation.application;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
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
import ro.scribemed.backend.consultation.infrastructure.ConsultationRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.infrastructure.PatientRepository;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobType;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

@Service
public class ConsultationService {

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/webm",
            "audio/wav",
            "audio/x-wav",
            "audio/mpeg",
            "audio/mp4",
            "audio/ogg"
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
            @Value("${scribemed.audio.max-size-bytes}") long maxAudioSizeBytes
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
    }

    @Transactional
    public ConsultationResponse createConsultation(CreateConsultationRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        Patient patient = patientRepository.findByIdAndTenant_Id(request.patientId(), request.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
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

        validateAudio(file);

        String objectKey = buildObjectKey(tenantId, consultationId, file.getOriginalFilename());
        StoredAudio storedAudio;
        try {
            storedAudio = audioStorageService.store(objectKey, file.getInputStream(), file.getSize());
        } catch (IOException error) {
            throw new IllegalStateException("Audio file could not be stored", error);
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
    public TranscriptResponse getTranscript(UUID consultationId, UUID tenantId) {
        getTenantConsultation(consultationId, tenantId);
        return transcriptRepository.findByConsultation_IdAndTenant_Id(consultationId, tenantId)
                .map(TranscriptResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Transcript not found"));
    }

    private Consultation getTenantConsultation(UUID consultationId, UUID tenantId) {
        return consultationRepository.findByIdAndTenant_Id(consultationId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Consultation not found"));
    }

    private void validateAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        if (file.getSize() > maxAudioSizeBytes) {
            throw new IllegalArgumentException("Audio file is too large");
        }
        if (file.getContentType() == null || !ALLOWED_AUDIO_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Audio file type is not supported");
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

    private List<ConsultationResponse> getAllConsultations() {

    }

}
