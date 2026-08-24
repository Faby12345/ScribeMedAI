package ro.scribemed.backend.patient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.domain.PatientSex;
import ro.scribemed.backend.patient.dto.PatientResponse;
import ro.scribemed.backend.patient.infrastructure.PatientRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;

class PatientServiceTests {

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private final PatientService patientService = new PatientService(
            patientRepository,
            tenantRepository,
            appUserRepository,
            auditService
    );

    @Test
    void createPatientStoresTenantScopedPatientAndRecordsSafeAuditEvent() {
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser actorUser = new AppUser(
                tenant,
                "doctor@example.com",
                "hash",
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        when(tenantRepository.findById(any())).thenReturn(Optional.of(tenant));
        when(appUserRepository.findByIdAndTenant_Id(any(), any())).thenReturn(Optional.of(actorUser));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.createPatient(new CreatePatientCommand(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "  Ana  ",
                "  Ionescu  ",
                LocalDate.of(1985, 3, 20),
                PatientSex.FEMALE,
                "  0712345678  ",
                "  ana@example.com  "
        ));

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient savedPatient = patientCaptor.getValue();

        assertThat(savedPatient.getTenant()).isEqualTo(tenant);
        assertThat(savedPatient.getFirstName()).isEqualTo("Ana");
        assertThat(savedPatient.getLastName()).isEqualTo("Ionescu");
        assertThat(savedPatient.getPhone()).isEqualTo("0712345678");
        assertThat(savedPatient.getEmail()).isEqualTo("ana@example.com");
        assertThat(response.firstName()).isEqualTo("Ana");
        assertThat(response.lastName()).isEqualTo("Ionescu");
        verify(auditService).record(
                eq(tenant),
                eq(actorUser),
                eq("PATIENT_CREATED"),
                eq("patient"),
                any(),
                eq(Map.of("status", "created"))
        );
    }

    @Test
    void createPatientRejectsActorFromAnotherTenant() {
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        when(tenantRepository.findById(any())).thenReturn(Optional.of(tenant));
        when(appUserRepository.findByIdAndTenant_Id(any(), any())).thenReturn(Optional.empty());

        CreatePatientCommand command = new CreatePatientCommand(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "Ana",
                "Ionescu",
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> patientService.createPatient(command))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAllPatientsReturnsRepositoryProjectionForTenant() {
        UUID tenantId = UUID.randomUUID();

        PatientResponse patient = new PatientResponse(
                UUID.randomUUID(),
                tenantId,
                "Ana",
                "Ionescu",
                LocalDate.of(1985, 3, 20),
                PatientSex.FEMALE,
                "0712345678",
                "ana@example.com",
                ro.scribemed.backend.patient.domain.PatientStatus.ACTIVE,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(patientRepository.findActiveResponsesByTenantId(tenantId))
                .thenReturn(List.of(patient));

        List<PatientResponse> result = patientService.getAllPatients(tenantId);

        assertThat(result).containsExactly(patient);
        verify(patientRepository).findActiveResponsesByTenantId(tenantId);
    }
    @Test
    void getAllPatientsDoesNotUseUnsafeEntityListLookup() {
        UUID tenantId = UUID.randomUUID();

        when(patientRepository.findActiveResponsesByTenantId(tenantId))
                .thenReturn(List.of());

        List<PatientResponse> result = patientService.getAllPatients(tenantId);

        assertThat(result).isEmpty();
        verify(patientRepository).findActiveResponsesByTenantId(tenantId);
        verify(patientRepository, never())
                .findByTenant_IdAndStatusOrderByLastNameAscFirstNameAsc(any(),
                        any());
    }

    @Test
    void getPatientReturnsTenantScopedPatien() {
        UUID patientId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        Patient patient = new Patient(
                tenant,
                "Ana",
                "Ionescu",
                LocalDate.of(1985, 3, 20),
                PatientSex.FEMALE,
                "0712345678",
                "ana@example.com"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(patientRepository.findPatientByIdAndTenantId(patientId, tenantId))
                .thenReturn(Optional.of(patient));

        PatientResponse result = patientService.getPatient(tenantId, patientId);

        assertThat(result.firstName()).isEqualTo("Ana");
        assertThat(result.lastName()).isEqualTo("Ionescu");
        verify(patientRepository).findPatientByIdAndTenantId(patientId, tenantId);
    }

    @Test
    void getPatientThrowsWhenPatientIsNotInTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(patientRepository.findPatientByIdAndTenantId(patientId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatient(tenantId, patientId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(patientRepository).findPatientByIdAndTenantId(patientId, tenantId);
    }
}
