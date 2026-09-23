package ro.scribemed.backend.knowledge.api;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.knowledge.application.KnowledgeDocumentIngestionService;
import ro.scribemed.backend.knowledge.application.KnowledgeService;
import ro.scribemed.backend.knowledge.dto.KnowledgeDocumentRequest;
import ro.scribemed.backend.knowledge.dto.KnowledgeDocumentResponse;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeDocumentIngestionService knowledgeDocumentIngestionService;
    private final KnowledgeService knowledgeService;


    public KnowledgeController(KnowledgeDocumentIngestionService knowledgeDocumentIngestionService, KnowledgeService knowledgeService) {
        this.knowledgeDocumentIngestionService = knowledgeDocumentIngestionService;
        this.knowledgeService = knowledgeService;
    }

    @PostMapping(
            value= "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") @Valid KnowledgeDocumentRequest dto,
            @AuthenticationPrincipal CurrentUser currentUser
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

    @GetMapping
    public ResponseEntity<Page<KnowledgeDocumentResponse>> getDocumentsForTenant(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(
                    size = 8,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ){
        return ResponseEntity.ok(
                knowledgeService.getActiveDocuments(
                        currentUser.tenantId(),
                        pageable
                )
        );
    }


}
