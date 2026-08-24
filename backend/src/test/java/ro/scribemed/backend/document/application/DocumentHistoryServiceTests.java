package ro.scribemed.backend.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.document.domain.ClinicalDocument;
import ro.scribemed.backend.document.dto.PatientDocumentSummaryResponse;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.infrastructure.PatientRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;

class DocumentHistoryServiceTests {

    private final ClinicalDocumentRepository clinicalDocumentRepository = mock(ClinicalDocumentRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final DocumentHistoryService documentHistoryService = new DocumentHistoryService(
            clinicalDocumentRepository,
            patientRepository
    );

    @Test
    void getPatientDocumentsReturnsDocumentSummariesForTenantOwnedPatient() {
        UUID tenantId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant consultationCreatedAt = Instant.parse("2026-08-24T10:00:00Z");
        Instant documentCreatedAt = Instant.parse("2026-08-24T10:05:00Z");
        Instant documentUpdatedAt = Instant.parse("2026-08-24T10:10:00Z");
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
        ReflectionTestUtils.setField(patient, "id", patientId);
        Consultation consultation = new Consultation(tenant, patient, doctor);
        ReflectionTestUtils.setField(consultation, "id", consultationId);
        ReflectionTestUtils.setField(consultation, "createdAt", consultationCreatedAt);
        ClinicalDocument document = new ClinicalDocument(tenant, consultation, "SOAP_NOTE");
        ReflectionTestUtils.setField(document, "id", documentId);
        ReflectionTestUtils.setField(document, "currentVersionNumber", 2);
        ReflectionTestUtils.setField(document, "createdAt", documentCreatedAt);
        ReflectionTestUtils.setField(document, "updatedAt", documentUpdatedAt);

        when(patientRepository.findByIdAndTenant_Id(patientId, tenantId))
                .thenReturn(Optional.of(patient));
        when(clinicalDocumentRepository.findByConsultation_Patient_IdAndTenant_IdOrderByConsultation_CreatedAtDesc(
                patientId,
                tenantId
        )).thenReturn(List.of(document));

        List<PatientDocumentSummaryResponse> response = documentHistoryService.getPatientDocuments(
                patientId,
                tenantId
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().documentId()).isEqualTo(documentId);
        assertThat(response.getFirst().consultationId()).isEqualTo(consultationId);
        assertThat(response.getFirst().documentType()).isEqualTo("SOAP_NOTE");
        assertThat(response.getFirst().currentVersionNumber()).isEqualTo(2);
        assertThat(response.getFirst().consultationCreatedAt()).isEqualTo(consultationCreatedAt);
        assertThat(response.getFirst().documentCreatedAt()).isEqualTo(documentCreatedAt);
        assertThat(response.getFirst().documentUpdatedAt()).isEqualTo(documentUpdatedAt);
    }

    @Test
    void getPatientDocumentsRequiresTenantOwnedPatient() {
        UUID tenantId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(patientRepository.findByIdAndTenant_Id(patientId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentHistoryService.getPatientDocuments(patientId, tenantId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
