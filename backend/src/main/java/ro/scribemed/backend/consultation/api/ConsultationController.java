package ro.scribemed.backend.consultation.api;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.scribemed.backend.consultation.application.*;
import ro.scribemed.backend.consultation.dto.AudioUploadResponse;
import ro.scribemed.backend.consultation.dto.ConsultationResponse;
import ro.scribemed.backend.consultation.dto.CreateConsultationRequest;
import ro.scribemed.backend.consultation.dto.NotesRequest;
import ro.scribemed.backend.consultation.dto.NotesResponse;
import ro.scribemed.backend.consultation.dto.TranscriptResponse;
import ro.scribemed.backend.identity.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    ResponseEntity<ConsultationResponse> createConsultation(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateConsultationApiRequest request
    ) {
        ConsultationResponse response = consultationService.createConsultation(new CreateConsultationRequest(
                currentUser.tenantId(),
                currentUser.userId(),
                request.patientId()
        ));

        return ResponseEntity
                .created(URI.create("/api/v1/consultations/" + response.id()))
                .body(response);
    }

    @PostMapping("/{consultationId}/patient-informed")
    ConsultationResponse confirmPatientInformed(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID consultationId
    ) {
        return consultationService.confirmPatientInformed(
                consultationId,
                currentUser.tenantId(),
                currentUser.userId()
        );
    }

    @PostMapping("/{consultationId}/audio")
    AudioUploadResponse uploadAudio(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID consultationId,
            @RequestParam("file") MultipartFile file
    ) {
        return consultationService.uploadAudio(
                consultationId,
                currentUser.tenantId(),
                currentUser.userId(),
                file
        );
    }

    @GetMapping
    ResponseEntity<Page<ConsultationResponse>> getAllConsultations(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(consultationService.getAllConsultations(currentUser.tenantId(), pageable));
    }

    @GetMapping("/{consultationId}")
    ConsultationResponse getConsultation(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID consultationId
    ) {
        return consultationService.getConsultation(consultationId, currentUser.tenantId());
    }

    @GetMapping("/{consultationId}/transcript")
    TranscriptResponse getTranscript(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID consultationId
    ) {
        return consultationService.getTranscript(consultationId, currentUser.tenantId());
    }

    @PostMapping("/{consultationId}/notes")
    ResponseEntity<NotesResponse> processNotes(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID consultationId,
            @RequestBody NotesRequest notesRequest
    ) {
        NotesResponse response =  consultationService.processNotes(
                notesRequest,
                currentUser.tenantId(),
                currentUser.userId(),
                consultationId);

        return ResponseEntity.accepted().body(response);

    }
}
