package ro.scribemed.backend.prescribedMedication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import ro.scribemed.backend.consultation.domain.ConsultationNotes;
import ro.scribemed.backend.medication.domain.Medication;
import ro.scribemed.backend.medication.infrastructure.MedicationRepository;
import ro.scribemed.backend.prescribedMedication.domain.PrescribedMedication;
import ro.scribemed.backend.prescribedMedication.dto.PrescribedMedicationRequest;
import ro.scribemed.backend.prescribedMedication.infrastructure.PrescribedMedicationRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;

class PrescribedMedicationServiceTests {

    @Test
    void createsOrderedCatalogSnapshotsFromDoctorInput() {
        Tenant tenant = new Tenant("Clinică test", TenantStatus.ACTIVE);
        ConsultationNotes consultationNotes = consultationNotes();
        Medication paracetamol = medication("CIM-1", "Paracetamol", false);
        Medication ibuprofen = medication("CIM-2", "Ibuprofen", false);
        RecordingMedicationRepository medicationRepository =
                new RecordingMedicationRepository(List.of(ibuprofen, paracetamol));
        RecordingPrescribedMedicationRepository prescribedRepository =
                new RecordingPrescribedMedicationRepository();
        PrescribedMedicationService service = new PrescribedMedicationService(
                medicationRepository.proxy(),
                prescribedRepository.proxy()
        );

        service.createPlan(
                tenant,
                consultationNotes,
                List.of(
                        request("CIM-1", " 500 mg "),
                        request("CIM-2", " 200 mg ")
                )
        );

        assertThat(medicationRepository.requestedCimCodes)
                .containsExactly("CIM-1", "CIM-2");
        assertThat(prescribedRepository.saved)
                .extracting(PrescribedMedication::getCatalogCimCode)
                .containsExactly("CIM-1", "CIM-2");
        assertThat(prescribedRepository.saved)
                .extracting(PrescribedMedication::getPosition)
                .containsExactly(0, 1);
        assertThat(prescribedRepository.saved.getFirst().getCommercialNameSnapshot())
                .isEqualTo("Paracetamol");
        assertThat(prescribedRepository.saved.getFirst().getDose()).isEqualTo("500 mg");
        assertThat(prescribedRepository.saved.getFirst().getAdministrationRoute()).isEqualTo("ORAL");
    }

    @Test
    void rejectsDuplicateCimCodesBeforeReadingTheCatalog() {
        RecordingMedicationRepository medicationRepository =
                new RecordingMedicationRepository(List.of());
        RecordingPrescribedMedicationRepository prescribedRepository =
                new RecordingPrescribedMedicationRepository();
        PrescribedMedicationService service = new PrescribedMedicationService(
                medicationRepository.proxy(),
                prescribedRepository.proxy()
        );

        assertThatThrownBy(() -> service.createPlan(
                new Tenant("Clinică test", TenantStatus.ACTIVE),
                consultationNotes(),
                List.of(request("CIM-1", "500 mg"), request("CIM-1", "500 mg"))
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(medicationRepository.findAllByIdCalls).isZero();
        assertThat(prescribedRepository.saved).isEmpty();
    }

    @Test
    void rejectsSuspendedMedicationWithoutSavingThePlan() {
        RecordingMedicationRepository medicationRepository =
                new RecordingMedicationRepository(List.of(medication("CIM-1", "Paracetamol", true)));
        RecordingPrescribedMedicationRepository prescribedRepository =
                new RecordingPrescribedMedicationRepository();
        PrescribedMedicationService service = new PrescribedMedicationService(
                medicationRepository.proxy(),
                prescribedRepository.proxy()
        );

        assertThatThrownBy(() -> service.createPlan(
                new Tenant("Clinică test", TenantStatus.ACTIVE),
                consultationNotes(),
                List.of(request("CIM-1", "500 mg"))
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(prescribedRepository.saved).isEmpty();
    }

    private ConsultationNotes consultationNotes() {
        return ConsultationNotes.create(null, null, null, null, null, null, null, null);
    }

    private PrescribedMedicationRequest request(String cimCode, String dose) {
        return new PrescribedMedicationRequest(
                cimCode,
                dose,
                " ORAL ",
                "De două ori pe zi",
                "7 zile",
                "14 comprimate",
                "După masă",
                null
        );
    }

    private Medication medication(String cimCode, String commercialName, boolean suspended) {
        return new Medication(
                cimCode,
                commercialName,
                commercialName + " substanță activă",
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
                suspended,
                false,
                LocalDate.of(2026, 1, 1)
        );
    }

    private static final class RecordingMedicationRepository {

        private final List<Medication> result;
        private int findAllByIdCalls;
        private List<String> requestedCimCodes = List.of();

        private RecordingMedicationRepository(List<Medication> result) {
            this.result = result;
        }

        private MedicationRepository proxy() {
            return (MedicationRepository) Proxy.newProxyInstance(
                    MedicationRepository.class.getClassLoader(),
                    new Class<?>[]{MedicationRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("findAllById")) {
                            findAllByIdCalls++;
                            List<String> codes = new ArrayList<>();
                            ((Iterable<?>) arguments[0]).forEach(code -> codes.add((String) code));
                            requestedCimCodes = List.copyOf(codes);
                            return result;
                        }
                        if (method.getName().equals("toString")) {
                            return "RecordingMedicationRepository";
                        }
                        throw new UnsupportedOperationException(
                                "Unexpected medication repository call: " + method.getName()
                        );
                    }
            );
        }
    }

    private static final class RecordingPrescribedMedicationRepository {

        private List<PrescribedMedication> saved = List.of();

        private PrescribedMedicationRepository proxy() {
            return (PrescribedMedicationRepository) Proxy.newProxyInstance(
                    PrescribedMedicationRepository.class.getClassLoader(),
                    new Class<?>[]{PrescribedMedicationRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("saveAll")) {
                            List<PrescribedMedication> captured = new ArrayList<>();
                            ((Iterable<?>) arguments[0]).forEach(item ->
                                    captured.add((PrescribedMedication) item)
                            );
                            saved = List.copyOf(captured);
                            return saved;
                        }
                        if (method.getName().equals("toString")) {
                            return "RecordingPrescribedMedicationRepository";
                        }
                        throw new UnsupportedOperationException(
                                "Unexpected prescribed-medication repository call: " + method.getName()
                        );
                    }
            );
        }
    }
}
