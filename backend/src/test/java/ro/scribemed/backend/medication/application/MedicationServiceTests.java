package ro.scribemed.backend.medication.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import ro.scribemed.backend.medication.dto.MedicationResponse;
import ro.scribemed.backend.medication.infrastructure.MedicationRepository;

class MedicationServiceTests {

    @Test
    void trimsTheQueryAndCapsTheResultLimitAtFive() {
        MedicationResponse medication = medicationResponse();
        RecordingRepository repository = new RecordingRepository(List.of(medication));
        MedicationService medicationService = new MedicationService(repository.proxy());

        List<MedicationResponse> result = medicationService
                .getMedicationByQuery("  paracetamol  ", 100);

        assertThat(repository.callCount).isEqualTo(1);
        assertThat(repository.query).isEqualTo("paracetamol");
        assertThat(repository.pageable.getPageNumber()).isZero();
        assertThat(repository.pageable.getPageSize()).isEqualTo(5);
        assertThat(result).containsExactly(medication);
    }

    @Test
    void doesNotSearchForQueriesShorterThanTwoCharacters() {
        RecordingRepository repository = new RecordingRepository(List.of());
        MedicationService medicationService = new MedicationService(repository.proxy());

        assertThat(medicationService.getMedicationByQuery(" p ", 5)).isEmpty();
        assertThat(medicationService.getMedicationByQuery(null, 5)).isEmpty();
        assertThat(repository.callCount).isZero();
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

    private static final class RecordingRepository {

        private final List<MedicationResponse> result;
        private int callCount;
        private String query;
        private Pageable pageable;

        private RecordingRepository(List<MedicationResponse> result) {
            this.result = result;
        }

        private MedicationRepository proxy() {
            return (MedicationRepository) Proxy.newProxyInstance(
                    MedicationRepository.class.getClassLoader(),
                    new Class<?>[]{MedicationRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("findByQuery")) {
                            callCount++;
                            query = (String) arguments[0];
                            pageable = (Pageable) arguments[1];
                            return result;
                        }
                        if (method.getName().equals("toString")) {
                            return "RecordingMedicationRepository";
                        }
                        throw new UnsupportedOperationException(
                                "Unexpected repository call: " + method.getName()
                        );
                    }
            );
        }
    }
}
