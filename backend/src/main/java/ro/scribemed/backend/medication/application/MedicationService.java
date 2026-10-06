package ro.scribemed.backend.medication.application;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ro.scribemed.backend.medication.dto.MedicationResponse;
import ro.scribemed.backend.medication.infrastructure.MedicationRepository;

import java.util.List;

@Service
public class MedicationService {
    private final MedicationRepository medicationRepository;


    public MedicationService(MedicationRepository medicationRepository) {
        this.medicationRepository = medicationRepository;
    }

    public Page<MedicationResponse> getAllMedication(Pageable page){
        return medicationRepository.findResponse(page);
    }

    public List<MedicationResponse> getMedicationByQuery(String query, int maxItems){
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        int safeMaxItems = Math.min(Math.max(maxItems, 1), 5);
        return medicationRepository.findByQuery(
                normalizedQuery,
                PageRequest.of(0, safeMaxItems)
        );
    }

}
