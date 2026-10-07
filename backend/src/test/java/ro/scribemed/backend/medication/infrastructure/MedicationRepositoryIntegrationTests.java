package ro.scribemed.backend.medication.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ro.scribemed.backend.medication.domain.Medication;
import ro.scribemed.backend.medication.dto.MedicationResponse;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MedicationRepositoryIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18")
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("scribemed_medication_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MedicationRepository medicationRepository;

    @Test
    void returnsTheFiveMostRelevantActiveMedicationResponses() {
        medicationRepository.saveAllAndFlush(List.of(
                medication("PARA", "Alt medicament", "Substanță", "A01", false),
                medication("CIM-EXACT-NAME", "para", "Substanță", "A02", false),
                medication("PARA-001", "Medicament cu cod prefix", "Substanță", "A03", false),
                medication("CIM-NAME-PREFIX", "Paracetamol", "Substanță", "A04", false),
                medication("CIM-SUBSTANCE", "Medicament", "Paracetamolum", "A05", false),
                medication("CIM-ATC", "Medicament ATC", "Substanță", "PARA-ATC", false),
                medication("CIM-CONTAINS", "Medicament para secundar", "Substanță", "A07", false),
                medication("CIM-SUSPENDED", "para", "Substanță", "A08", true)
        ));

        List<MedicationResponse> result = medicationRepository.findByQuery(
                "PaRa",
                PageRequest.of(0, 5)
        );

        assertThat(result)
                .extracting(MedicationResponse::cimCode)
                .containsExactly(
                        "PARA",
                        "CIM-EXACT-NAME",
                        "PARA-001",
                        "CIM-NAME-PREFIX",
                        "CIM-SUBSTANCE"
                );
        assertThat(result).allMatch(medication -> !medication.authorizationSuspended());
    }

    private Medication medication(
            String cimCode,
            String commercialName,
            String activeSubstance,
            String atcCode,
            boolean authorizationSuspended
    ) {
        return new Medication(
                cimCode,
                commercialName,
                activeSubstance,
                "COMPRIMAT",
                "500 mg",
                "Producător",
                "Deținător",
                atcCode,
                "Acțiune terapeutică",
                "PRF",
                "Autorizație",
                "Cutie",
                "20 comprimate",
                "Valabil",
                false,
                false,
                true,
                authorizationSuspended,
                false,
                LocalDate.of(2026, 1, 1)
        );
    }
}
