package ro.scribemed.backend.medication.application;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ro.scribemed.backend.medication.dto.MedicationResponse;
import ro.scribemed.backend.medication.infrastructure.MedicationRepository;

@Service
public class MedicationService {
    private final MedicationRepository medicationRepository;


    public MedicationService(MedicationRepository medicationRepository) {
        this.medicationRepository = medicationRepository;
    }

    public Page<MedicationResponse> getAllMedication(Pageable page){
        return medicationRepository.findResponse(page);
    }


}
