package ro.scribemed.backend.medication.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ro.scribemed.backend.medication.application.MedicationService;
import ro.scribemed.backend.medication.dto.MedicationResponse;

class MedicationControllerTests {

    @Test
    void returnsAtMostFiveMedicationSearchResults() {
        MedicationResponse medication = medicationResponse();
        StubMedicationService medicationService = new StubMedicationService(List.of(medication));
        MedicationController controller = new MedicationController(medicationService);

        ResponseEntity<List<MedicationResponse>> response = controller
                .getMedicationByQuery("paracetamol");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(medication);
        assertThat(medicationService.query).isEqualTo("paracetamol");
        assertThat(medicationService.maxItems).isEqualTo(5);
    }

    private MedicationResponse medicationResponse() {
        return new MedicationResponse(
                "CIM-1",
                "Paracetamol",
                "Paracetamolum",
                "COMPRIMAT",
                "500 mg",
                "Producător",
                "Deținător",
                "N02BE01",
                "Analgezic",
                "PRF",
                "Autorizație",
                "Cutie",
                "20 comprimate",
                "Valabil",
                false,
                false,
                true,
                false,
                false,
                LocalDate.of(2026, 1, 1)
        );
    }

    private static final class StubMedicationService extends MedicationService {

        private final List<MedicationResponse> result;
        private String query;
        private int maxItems;

        private StubMedicationService(List<MedicationResponse> result) {
            super(null);
            this.result = result;
        }

        @Override
        public List<MedicationResponse> getMedicationByQuery(String query, int maxItems) {
            this.query = query;
            this.maxItems = maxItems;
            return result;
        }
    }
}
