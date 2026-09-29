package ro.scribemed.backend.medication.infrastructure.importdata.application;

import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ro.scribemed.backend.medication.infrastructure.importdata.configuration.MedicationImportProperties;
import ro.scribemed.backend.medication.infrastructure.importdata.configuration.MedicationImportRow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Component
@ConditionalOnProperty(
        prefix = "scribemed.medications.import",
        name = "source",
        havingValue = "excel"
)
public class ExcelMedicationImporter implements MedicationImporter {

    private static final DateTimeFormatter SHORT_RO_DATE_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("d.M.")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                    .toFormatter();
    private final MedicationImportProperties medicationImportProperties;
    private static final int BATCH_SIZE = 500;
    private final MedicationImportValidator validator;
    private final MedicationJdbcWriter writer;

    public ExcelMedicationImporter(MedicationImportProperties medicationImportProperties, MedicationImportValidator validator, MedicationJdbcWriter writer) {
        this.medicationImportProperties = medicationImportProperties;
        this.validator = validator;
        this.writer = writer;
    }


    @Override
    public MedicationImportResult importMedication(){
        Path file = medicationImportProperties.filePath();

        int total = 0;
        int imported = 0;
        int skipped = 0;

        DataFormatter formatter = new DataFormatter();

        try (
                InputStream input = Files.newInputStream(file);
                Workbook workbook = WorkbookFactory.create(input)
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet.getRow(0), formatter);
            List<MedicationImportRow> batch = new ArrayList<>(BATCH_SIZE);

            // Row 0 is assumed to contain headers.
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row excelRow = sheet.getRow(rowIndex);

                if (excelRow == null) {
                    continue;
                }

                total++;

                MedicationImportRow row =
                        mapRow(excelRow, formatter);

                if (!validator.isValid(row)) {
                    skipped++;
                    continue;
                }

                batch.add(row);

                if (batch.size() == BATCH_SIZE) {
                    writer.write(batch);
                    imported += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                writer.write(batch);
                imported += batch.size();
            }

            return new MedicationImportResult(total, imported, skipped);
        } catch (IOException exception) {
            throw new MedicationImportException(
                    "Could not import medication Excel file: " + file,
                    exception
            );
        }
    }

    private MedicationImportRow mapRow(
            Row row,
            DataFormatter formatter
    ) {
        return new MedicationImportRow(
                value(row, 0, formatter),
                value(row, 1, formatter),
                value(row, 2, formatter),
                value(row, 3, formatter),
                value(row, 4, formatter),
                value(row, 5, formatter),
                value(row, 6, formatter),
                value(row, 7, formatter),
                value(row, 8, formatter),
                value(row, 9, formatter),
                value(row, 10, formatter),
                value(row, 11, formatter),
                value(row, 12, formatter),
                value(row, 13, formatter),
                booleanValue(row, 14, formatter),
                booleanValue(row, 15, formatter),
                booleanValue(row, 16, formatter),
                booleanValue(row, 17, formatter),
                booleanValue(row, 18, formatter),
                dateValue(row, 19, formatter)
        );
    }

    private void validateHeader(Row header, DataFormatter formatter) {
        if (header == null) {
            throw new MedicationImportException("Medication Excel file has no header row", null);
        }

        for (int column = 0; column < 20; column++) {
            if (value(header, column, formatter) == null) {
                throw new MedicationImportException(
                        "Medication Excel file is missing a required header at column " + column,
                        null
                );
            }
        }
    }

    private String value(
            Row row,
            int column,
            DataFormatter formatter
    ) {
        Cell cell = row.getCell(
                column,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
        );

        if (cell == null) {
            return null;
        }

        String value = formatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private boolean booleanValue(
            Row row,
            int column,
            DataFormatter formatter
    ) {
        String value = value(row, column, formatter);
        if (value == null) {
            return false;
        }

        return switch (value.toLowerCase(Locale.ROOT)) {
            case "1", "true", "da", "x" -> true;
            case "0", "false", "nu" -> false;
            default -> throw new MedicationImportException(
                    "Invalid boolean value at column " + column + ": " + value,
                    null
            );
        };
    }

    private LocalDate dateValue(
            Row row,
            int column,
            DataFormatter formatter
    ) {
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        String value = value(row, column, formatter);
        if (value == null) {
            return null;
        }

        for (DateTimeFormatter dateFormat : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d.M.uuuu"),
                SHORT_RO_DATE_FORMAT,
                DateTimeFormatter.ofPattern("d/M/uuuu")
        )) {
            try {
                return LocalDate.parse(value, dateFormat);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }

        throw new MedicationImportException(
                "Invalid date value at column " + column + ": " + value,
                null
        );
    }
}
