package ro.scribemed.backend.consultation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import ro.scribemed.backend.audio.application.AudioStorageService;
import ro.scribemed.backend.audio.application.StoredAudio;
import ro.scribemed.backend.audio.domain.ConsultationAudio;
import ro.scribemed.backend.audio.infrastructure.ConsultationAudioRepository;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.consultation.domain.ConsultationStatus;
import ro.scribemed.backend.consultation.application.exception.AudioFileTooLargeException;
import ro.scribemed.backend.consultation.application.exception.ConsultationNotFoundException;
import ro.scribemed.backend.consultation.application.exception.ConsultationStateException;
import ro.scribemed.backend.consultation.application.exception.PatientNotFoundException;
import ro.scribemed.backend.consultation.application.exception.UnsupportedAudioTypeException;
import ro.scribemed.backend.consultation.dto.AudioUploadResponse;
import ro.scribemed.backend.consultation.dto.ConsultationResponse;
import ro.scribemed.backend.consultation.dto.CreateConsultationRequest;
import ro.scribemed.backend.consultation.dto.NotesRequest;
import ro.scribemed.backend.consultation.dto.NotesResponse;
import ro.scribemed.backend.consultation.infrastructure.ConsultationNotesRepository;
import ro.scribemed.backend.consultation.infrastructure.ConsultationRepository;
import ro.scribemed.backend.consultation.infrastructure.HuggingFaceClinicalNoteGenerationProvider;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.infrastructure.PatientRepository;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobStatus;
import ro.scribemed.backend.processing.domain.ProcessingJobType;
import ro.scribemed.backend.processing.infrastructure.ProcessingJobRepository;
import ro.scribemed.backend.prescribedMedication.application.PrescribedMedicationService;
import ro.scribemed.backend.prescribedMedication.dto.PrescribedMedicationRequest;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;
import ro.scribemed.backend.transcription.infrastructure.ConsultationTranscriptRepository;

class ConsultationServiceTests {

    private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final ConsultationAudioRepository audioRepository = mock(ConsultationAudioRepository.class);
    private final ProcessingJobRepository processingJobRepository = mock(ProcessingJobRepository.class);
    private final ConsultationTranscriptRepository transcriptRepository = mock(ConsultationTranscriptRepository.class);
    private final AudioStorageService audioStorageService = mock(AudioStorageService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ConsultationNotesRepository consultationNotesRepository = mock(ConsultationNotesRepository.class);
    private final PrescribedMedicationService prescribedMedicationService = mock(PrescribedMedicationService.class);

    private final ConsultationService consultationService = new ConsultationService(
            consultationRepository,
            patientRepository,
            appUserRepository,
            tenantRepository,
            audioRepository,
            processingJobRepository,
            transcriptRepository,
            audioStorageService,
            auditService,
            25_000_000,
            consultationNotesRepository,
            prescribedMedicationService
    );

    @Test
    void createConsultationRequiresTenantOwnedPatient() {
        UUID tenantId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(patientRepository.findByIdAndTenant_Id(patientId, tenantId)).thenReturn(Optional.of(patient));
        when(appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)).thenReturn(Optional.of(doctor));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.createConsultation(new CreateConsultationRequest(
                tenantId,
                actorUserId,
                patientId
        ));

        ArgumentCaptor<Consultation> consultationCaptor = ArgumentCaptor.forClass(Consultation.class);
        verify(consultationRepository).save(consultationCaptor.capture());
        Consultation savedConsultation = consultationCaptor.getValue();

        assertThat(savedConsultation.getTenant()).isEqualTo(tenant);
        assertThat(savedConsultation.getPatient()).isEqualTo(patient);
        assertThat(savedConsultation.getDoctorUser()).isEqualTo(doctor);
        assertThat(savedConsultation.getStatus()).isEqualTo(ConsultationStatus.CREATED);
        assertThat(response.status()).isEqualTo(ConsultationStatus.CREATED);
        verify(auditService).record(
                eq(tenant),
                eq(doctor),
                eq("CONSULTATION_CREATED"),
                eq("consultation"),
                any(),
                eq(Map.of("status", "created"))
        );
    }

    @Test
    void createConsultationRejectsPatientFromAnotherTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(patientRepository.findByIdAndTenant_Id(patientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultationService.createConsultation(new CreateConsultationRequest(
                tenantId,
                UUID.randomUUID(),
                patientId
        ))).isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void uploadAudioStoresFileAndQueuesTranscriptionJob() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        consultation.markPatientInformed(Instant.now());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.webm",
                "audio/webm",
                "audio".getBytes()
        );
        when(consultationRepository.findByIdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(consultation));
        when(appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)).thenReturn(Optional.of(doctor));
        when(audioStorageService.store(any(), any(ByteArrayInputStream.class), eq(file.getSize())))
                .thenReturn(new StoredAudio(
                        "tenant/%s/consultation/%s/audio.webm".formatted(tenantId, consultationId),
                        "checksum",
                        file.getSize()
                ));
        when(audioRepository.save(any(ConsultationAudio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processingJobRepository.save(any(ProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AudioUploadResponse response = consultationService.uploadAudio(
                consultationId,
                tenantId,
                actorUserId,
                file
        );

        ArgumentCaptor<ProcessingJob> jobCaptor = ArgumentCaptor.forClass(ProcessingJob.class);
        verify(processingJobRepository).save(jobCaptor.capture());

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.AUDIO_UPLOADED);
        assertThat(jobCaptor.getValue().getJobType()).isEqualTo(ProcessingJobType.TRANSCRIPTION);
        assertThat(response.status()).isEqualTo(ConsultationStatus.AUDIO_UPLOADED);
    }

    @Test
    void uploadAudioRequiresPatientInformedConfirmation() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.webm",
                "audio/webm",
                "audio".getBytes()
        );
        when(consultationRepository.findByIdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(consultation));
        when(appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> consultationService.uploadAudio(
                consultationId,
                tenantId,
                actorUserId,
                file
        )).isInstanceOf(ConsultationStateException.class);

        verify(audioStorageService, never()).store(any(), any(), anyLong());
        verify(audioRepository, never()).save(any());
        verify(processingJobRepository, never()).save(any());
    }

    @Test
    void uploadAudioRequiresTenantOwnedConsultation() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.webm",
                "audio/webm",
                "audio".getBytes()
        );
        when(consultationRepository.findByIdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultationService.uploadAudio(
                consultationId,
                tenantId,
                actorUserId,
                file
        )).isInstanceOf(ConsultationNotFoundException.class);

        verify(audioStorageService, never()).store(any(), any(), anyLong());
        verify(audioRepository, never()).save(any());
        verify(processingJobRepository, never()).save(any());
    }

    @Test
    void uploadAudioRejectsUnsupportedContentType() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        consultation.markPatientInformed(Instant.now());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "audio".getBytes()
        );
        when(consultationRepository.findByIdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(consultation));
        when(appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> consultationService.uploadAudio(
                consultationId,
                tenantId,
                actorUserId,
                file
        )).isInstanceOf(UnsupportedAudioTypeException.class);

        verify(audioStorageService, never()).store(any(), any(), anyLong());
        verify(audioRepository, never()).save(any());
        verify(processingJobRepository, never()).save(any());
    }

    @Test
    void uploadAudioRejectsOversizedFile() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        consultation.markPatientInformed(Instant.now());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.webm",
                "audio/webm",
                "audio".getBytes()
        );
        ConsultationService serviceWithSmallAudioLimit = new ConsultationService(
                consultationRepository,
                patientRepository,
                appUserRepository,
                tenantRepository,
                audioRepository,
                processingJobRepository,
                transcriptRepository,
                audioStorageService,
                auditService,
                3,
                consultationNotesRepository,
                prescribedMedicationService
        );
        when(consultationRepository.findByIdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(consultation));
        when(appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> serviceWithSmallAudioLimit.uploadAudio(
                consultationId,
                tenantId,
                actorUserId,
                file
        )).isInstanceOf(AudioFileTooLargeException.class);

        verify(audioStorageService, never()).store(any(), any(), anyLong());
        verify(audioRepository, never()).save(any());
        verify(processingJobRepository, never()).save(any());
    }

    @Test
    void getAllConsultationsReturnsTenantScopedRepositoryProjection() {
        UUID tenantId = UUID.randomUUID();
        ConsultationResponse consultation = new ConsultationResponse(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "Ana",
                "Ionescu",
                UUID.randomUUID(),
                ConsultationStatus.CREATED,
                null,
                Instant.now(),
                Instant.now()
        );

        PageRequest pageable = PageRequest.of(0, 20);
        when(consultationRepository.findResponsesByTenantId(tenantId, pageable))
                .thenReturn(new PageImpl<>(List.of(consultation), pageable, 1));

        Page<ConsultationResponse> result = consultationService.getAllConsultations(tenantId, pageable);

        assertThat(result.getContent()).containsExactly(consultation);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(consultationRepository).findResponsesByTenantId(tenantId, pageable);
        verify(consultationRepository, never()).findAll();
    }

    @Test
    void processNotesCreatesMedicationPlanBeforeQueuingDocumentGeneration() {
        UUID tenantId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser doctor = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        Patient patient = new Patient(tenant, "Ana", "Ionescu", null, null, null, null);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        PrescribedMedicationRequest medication = new PrescribedMedicationRequest(
                "CIM-1",
                "500 mg",
                "ORAL",
                "De două ori pe zi",
                "7 zile",
                "14 comprimate",
                "După masă",
                null
        );
        NotesRequest request = new NotesRequest(
                "Motiv",
                "Evaluare",
                "Istoric",
                "Obiectiv",
                "Plan",
                List.of(medication)
        );
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(appUserRepository.findByIdAndTenant_Id(actorUserId, tenantId)).thenReturn(Optional.of(doctor));
        when(consultationRepository.findByIdAndTenant_Id(consultationId, tenantId))
                .thenReturn(Optional.of(consultation));
        when(consultationNotesRepository.save(any(ConsultationNotes.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(processingJobRepository.save(any(ProcessingJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotesResponse response = consultationService.processNotes(
                request,
                tenantId,
                actorUserId,
                consultationId
        );

        ArgumentCaptor<ConsultationNotes> notesCaptor = ArgumentCaptor.forClass(ConsultationNotes.class);
        verify(consultationNotesRepository).save(notesCaptor.capture());
        verify(prescribedMedicationService).createPlan(
                tenant,
                notesCaptor.getValue(),
                request.medications()
        );
        verify(processingJobRepository).save(any(ProcessingJob.class));
        assertThat(response.status()).isEqualTo(ProcessingJobStatus.PENDING);
    }
}
