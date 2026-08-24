package ro.scribemed.backend.patient.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.domain.PatientStatus;
import ro.scribemed.backend.patient.dto.PatientResponse;
import ro.scribemed.backend.patient.infrastructure.PatientRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;

    public PatientService(
            PatientRepository patientRepository,
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            AuditService auditService
    ) {
        this.patientRepository = patientRepository;
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PatientResponse createPatient(CreatePatientCommand command) {
        Tenant tenant = tenantRepository.findById(command.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        AppUser actorUser = appUserRepository.findByIdAndTenant_Id(command.actorUserId(), command.tenantId())
                .orElseThrow(() -> new AccessDeniedException("Actor user is not part of the tenant"));

        if(patientRepository.existsByTenant_IdAndEmailIgnoreCase(command.tenantId(), command.email())){
            throw new EntityExistsException("Email already use with another account!");
        }

        Patient patient = patientRepository.save(new Patient(
                tenant,
                normalizeRequired(command.firstName()),
                normalizeRequired(command.lastName()),
                command.birthDate(),
                command.sex(),
                normalizeOptional(command.phone()),
                normalizeOptional(command.email())
        ));

        auditService.record(
                tenant,
                actorUser,
                "PATIENT_CREATED",
                "patient",
                patient.getId(),
                Map.of("status", "created")
        );

        return PatientResponse.from(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients(UUID tenantId) {
        return patientRepository.findActiveResponsesByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatient(UUID tenantId, UUID patientId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found!"));

        Patient patient = patientRepository.findPatientByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() ->
                    new EntityNotFoundException("Patient not found!")
                );

        return PatientResponse.from(patient);
    }



    private String normalizeRequired(String value) {
        return value.strip();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
