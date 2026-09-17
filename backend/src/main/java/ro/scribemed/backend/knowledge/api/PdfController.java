package ro.scribemed.backend.knowledge.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.knowledge.application.PdfService;
import ro.scribemed.backend.knowledge.dto.StoredPdf;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/pdf")
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping(
            value= "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StoredPdf> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentUser currentUser
            ) throws IOException
    {
        return ResponseEntity.ok(
                pdfService.store(
                        currentUser.tenantId(),
                        file.getInputStream(),
                        file.getSize()
                )
        );
    }
}
