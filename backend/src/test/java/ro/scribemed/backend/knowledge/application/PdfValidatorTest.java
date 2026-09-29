package ro.scribemed.backend.knowledge.application;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import ro.scribemed.backend.knowledge.application.pdf.PdfValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfValidatorTest {

    @Test
    void acceptsAReadablePdf() throws IOException {
        byte[] pdf = createPdf();

        assertDoesNotThrow(() -> PdfValidator.validate(
                new ByteArrayInputStream(pdf),
                pdf.length
        ));
    }

    @Test
    void rejectsAnEmptyFile() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PdfValidator.validate(new ByteArrayInputStream(new byte[0]), 0)
        );
    }

    @Test
    void rejectsAFileThatOnlyClaimsToBeAPdf() {
        byte[] content = "%PDF-not-a-real-pdf"
                .getBytes(StandardCharsets.US_ASCII);

        assertThrows(
                IllegalArgumentException.class,
                () -> PdfValidator.validate(
                        new ByteArrayInputStream(content),
                        content.length
                )
        );
    }

    @Test
    void rejectsContentWithoutThePdfHeader() {
        byte[] content = "ordinary text"
                .getBytes(StandardCharsets.US_ASCII);

        assertThrows(
                IllegalArgumentException.class,
                () -> PdfValidator.validate(
                        new ByteArrayInputStream(content),
                        content.length
                )
        );
    }

    @Test
    void rejectsADeclaredSizeThatDoesNotMatchTheStream() throws IOException {
        byte[] pdf = createPdf();

        assertThrows(
                IllegalArgumentException.class,
                () -> PdfValidator.validate(
                        new ByteArrayInputStream(pdf),
                        pdf.length - 1L
                )
        );
    }

    @Test
    void rejectsADeclaredSizeAboveTheLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PdfValidator.validate(
                        new ByteArrayInputStream(new byte[]{1}),
                        PdfValidator.MAX_PDF_SIZE_BYTES + 1
                )
        );
    }

    private byte[] createPdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
