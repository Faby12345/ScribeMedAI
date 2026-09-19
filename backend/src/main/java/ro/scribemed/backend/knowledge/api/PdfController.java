package ro.scribemed.backend.knowledge.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.knowledge.application.KnowledgeDocumentIngestionService;
import ro.scribemed.backend.knowledge.application.PdfService;
import ro.scribemed.backend.knowledge.dto.KnowledgeDocumentRequest;
import ro.scribemed.backend.knowledge.dto.StoredPdf;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/pdf")
public class PdfController {

    private final KnowledgeDocumentIngestionService knowledgeDocumentIngestionService;


    public PdfController(KnowledgeDocumentIngestionService knowledgeDocumentIngestionService) {
        this.knowledgeDocumentIngestionService = knowledgeDocumentIngestionService;
    }

    @PostMapping(
            value= "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AppUser currentUser,
            @RequestBody  KnowledgeDocumentRequest dto
            ) throws IOException
    {
        knowledgeDocumentIngestionService.process(
            currentUser,
            file.getInputStream(),
            file.getSize(),
            file.getOriginalFilename(),
            dto
        );

        return ResponseEntity
                .accepted()
                .build();

    }
}
