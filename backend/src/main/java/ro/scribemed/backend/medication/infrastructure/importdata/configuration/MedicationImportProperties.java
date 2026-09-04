package ro.scribemed.backend.medication.infrastructure.importdata.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "scribemed.medications.import")
public record MedicationImportProperties(
        boolean enabled,
        String source,
        Path filePath
) {
}
