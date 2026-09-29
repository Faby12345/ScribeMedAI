package ro.scribemed.backend.medication.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.scribemed.backend.medication.application.MedicationService;
import ro.scribemed.backend.medication.dto.MedicationResponse;

@RestController
@RequestMapping("/api/v1/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }


    @GetMapping
    public ResponseEntity<Page<MedicationResponse>> getAllMedication(
            @PageableDefault(
                    size = 20,
                    sort = "commercialName",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(medicationService.getAllMedication(pageable));
    }

}
