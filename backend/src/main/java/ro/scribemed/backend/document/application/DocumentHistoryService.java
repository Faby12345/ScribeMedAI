package ro.scribemed.backend.document.application;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.document.dto.PatientDocumentSummaryResponse;
import ro.scribemed.backend.document.infrastructure.ClinicalDocumentRepository;
import ro.scribemed.backend.patient.infrastructure.PatientRepository;

@Service
public class DocumentHistoryService {

    private final ClinicalDocumentRepository clinicalDocumentRepository;
    private final PatientRepository patientRepository;

    public DocumentHistoryService(
            ClinicalDocumentRepository clinicalDocumentRepository,
            PatientRepository patientRepository
    ) {
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientDocumentSummaryResponse> getPatientDocuments(UUID patientId, UUID tenantId) {
        patientRepository.findByIdAndTenant_Id(patientId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        return clinicalDocumentRepository
                .findByConsultation_Patient_IdAndTenant_IdOrderByConsultation_CreatedAtDesc(
                        patientId,
                        tenantId
                )
                .stream()
                .map(PatientDocumentSummaryResponse::from)
                .toList();
    }
}
