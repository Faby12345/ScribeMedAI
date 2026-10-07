package ro.scribemed.backend.prescribedMedication.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.medication.domain.Medication;
import ro.scribemed.backend.medication.infrastructure.MedicationRepository;
import ro.scribemed.backend.prescribedMedication.domain.PrescribedMedication;
import ro.scribemed.backend.prescribedMedication.dto.PrescribedMedicationRequest;
import ro.scribemed.backend.prescribedMedication.infrastructure.PrescribedMedicationRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Service
public class PrescribedMedicationService {

    private static final int MAX_MEDICATIONS_PER_PLAN = 20;

    private final MedicationRepository medicationRepository;
    private final PrescribedMedicationRepository prescribedMedicationRepository;

    public PrescribedMedicationService(
            MedicationRepository medicationRepository,
            PrescribedMedicationRepository prescribedMedicationRepository
    ) {
        this.medicationRepository = medicationRepository;
        this.prescribedMedicationRepository = prescribedMedicationRepository;
    }

    public void createPlan(
            Tenant tenant,
            ConsultationNotes consultationNotes,
            List<PrescribedMedicationRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        if (requests.size() > MAX_MEDICATIONS_PER_PLAN) {
            throw new IllegalArgumentException("Medication plan exceeds the maximum item count");
        }

        List<String> cimCodes = normalizedUniqueCimCodes(requests);
        Map<String, Medication> medicationsByCimCode = new HashMap<>();
        medicationRepository.findAllById(cimCodes)
                .forEach(medication -> medicationsByCimCode.put(medication.getCimCode(), medication));

        List<PrescribedMedication> prescribedMedications = new ArrayList<>(requests.size());
        for (int position = 0; position < requests.size(); position++) {
            prescribedMedications.add(toPrescribedMedication(
                    tenant,
                    consultationNotes,
                    requests.get(position),
                    medicationsByCimCode,
                    position
                ));
        }

        prescribedMedicationRepository.saveAll(prescribedMedications);
    }

    private List<String> normalizedUniqueCimCodes(List<PrescribedMedicationRequest> requests) {
        Set<String> uniqueCimCodes = new HashSet<>();

        return requests.stream()
                .map(request -> {
                    if (request == null) {
                        throw new IllegalArgumentException("Medication plan contains an empty item");
                    }
                    return request;
                })
                .map(request -> requireText(request.cimCode(), "cimCode"))
                .map(String::trim)
                .peek(cimCode -> {
                    if (!uniqueCimCodes.add(cimCode)) {
                        throw new IllegalArgumentException("Medication plan contains duplicate CIM codes");
                    }
                })
                .toList();
    }

    private PrescribedMedication toPrescribedMedication(
            Tenant tenant,
            ConsultationNotes consultationNotes,
            PrescribedMedicationRequest request,
            Map<String, Medication> medicationsByCimCode,
            int position
    ) {
        String cimCode = request.cimCode().trim();
        Medication medication = medicationsByCimCode.get(cimCode);
        if (medication == null) {
            throw new IllegalArgumentException("Medication catalog entry was not found");
        }
        if (medication.isAuthorizationSuspended()) {
            throw new IllegalArgumentException("Suspended medication cannot be prescribed");
        }

        return new PrescribedMedication(
                tenant,
                consultationNotes,
                position,
                medication.getCimCode(),
                medication.getCommercialName(),
                medication.getActiveSubstance(),
                medication.getPharmaceuticalForm(),
                medication.getConcentration(),
                medication.getPrescriptionType(),
                requireText(request.dose(), "dose"),
                requireText(request.administrationRoute(), "administrationRoute"),
                requireText(request.frequency(), "frequency"),
                requireText(request.duration(), "duration"),
                normalizeOptionalText(request.quantity()),
                normalizeOptionalText(request.instructions()),
                normalizeOptionalText(request.notes())
        );
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
