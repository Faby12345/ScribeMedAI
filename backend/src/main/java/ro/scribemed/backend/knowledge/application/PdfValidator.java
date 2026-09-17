package ro.scribemed.backend.knowledge.application;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class PdfValidator {

    static final long MAX_PDF_SIZE_BYTES = 25L * 1024 * 1024;

    private static final byte[] PDF_HEADER = "%PDF-"
            .getBytes(StandardCharsets.US_ASCII);

    private PdfValidator() {
    }

    /**
     * Validates and consumes the supplied stream. The caller remains responsible
     * for closing it and must open a new stream if the PDF is needed afterwards.
     */
    public static void validate(InputStream inputStream, long sizeBytes)
            throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("PDF input stream must not be null");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("PDF file must not be empty");
        }
        if (sizeBytes > MAX_PDF_SIZE_BYTES) {
            throw new IllegalArgumentException("PDF file exceeds the maximum size");
        }

        byte[] pdfBytes = inputStream.readNBytes((int) MAX_PDF_SIZE_BYTES + 1);
        if (pdfBytes.length > MAX_PDF_SIZE_BYTES) {
            throw new IllegalArgumentException("PDF file exceeds the maximum size");
        }
        if (pdfBytes.length != sizeBytes) {
            throw new IllegalArgumentException("PDF file size does not match its content");
        }
        if (pdfBytes.length < PDF_HEADER.length
                || !Arrays.equals(
                        PDF_HEADER,
                        Arrays.copyOf(pdfBytes, PDF_HEADER.length)
                )) {
            throw new IllegalArgumentException("Uploaded file is not a PDF");
        }

        validateStructure(pdfBytes);
    }

    private static void validateStructure(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                        "Encrypted PDF files are not supported"
                );
            }
            if (document.getNumberOfPages() == 0) {
                throw new IllegalArgumentException(
                        "PDF file must contain at least one page"
                );
            }
            if (!document.getCurrentAccessPermission().canExtractContent()) {
                throw new IllegalArgumentException(
                        "PDF file does not allow text extraction"
                );
            }
        } catch (IOException error) {
            throw new IllegalArgumentException(
                    "Uploaded file is not a readable PDF",
                    error
            );
        }
    }
}
