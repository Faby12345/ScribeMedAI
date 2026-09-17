package ro.scribemed.backend.knowledge.application;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.knowledge.dto.ExtractedPage;
import ro.scribemed.backend.knowledge.dto.StoredPdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public abstract class PdfService {

    public static List<ExtractedPage> extract(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<ExtractedPage> pages = new ArrayList<>();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = stripper.getText(document);
                pages.add(new ExtractedPage(page, text));
            }

            return pages;
        }
    }
    public String buildObjectKey(UUID tenantId) {
        UUID storageId = UUID.randomUUID();

        return "tenant/%s/knowledge/%s.pdf".formatted(
                tenantId,
                storageId
        );
    }


    public abstract StoredPdf store(UUID tenantId, InputStream inputStream, long sizeBytes) throws IOException;


    public abstract byte[] read(String objectKey) throws IOException;
}
