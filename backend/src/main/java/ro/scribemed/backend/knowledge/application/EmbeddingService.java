package ro.scribemed.backend.knowledge.application;

import org.springframework.stereotype.Service;
import ro.scribemed.backend.knowledge.application.pdf.PdfService;
import ro.scribemed.backend.knowledge.dto.ExtractedPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingProvider embeddingProvider;

    private final Path GHID_PSIHOLOGIE_FILE_PATH =  Path.of("/Users/turlefabian/Desktop/Ghiduri Psihiatrie-Anexa 1_8795_6756.pdf");

    public EmbeddingService(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }


    private void process() throws IOException {

        byte[] pdfBytes = Files.readAllBytes(GHID_PSIHOLOGIE_FILE_PATH);

        List<ExtractedPage> extractedPages = PdfService.extract(pdfBytes);




    }
}
