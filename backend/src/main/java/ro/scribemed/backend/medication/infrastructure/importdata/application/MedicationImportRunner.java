package ro.scribemed.backend.medication.infrastructure.importdata.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "scribemed.medications.import",
        name = "enabled",
        havingValue = "true"
)
public class MedicationImportRunner implements ApplicationRunner {

    private final MedicationImporter medicationImporter;
    private static final Logger log = LoggerFactory.getLogger(MedicationImportRunner.class);



    public MedicationImportRunner(MedicationImporter medicationImporter) {
        this.medicationImporter = medicationImporter;
    }

    @Override
    public void run(ApplicationArguments args) {
        MedicationImportResult result = medicationImporter.importMedication();

        log.info(
                "medication_import_completed total={} written={} skipped={}",
                result.total(),
                result.written(),
                result.skipped()
        );
    }
}
