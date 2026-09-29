package ro.scribemed.backend.patient.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.patient.application.CreatePatientCommand;
import ro.scribemed.backend.patient.dto.CreatePatientRequest;
import ro.scribemed.backend.patient.dto.PatientResponse;
import ro.scribemed.backend.patient.application.PatientService;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    ResponseEntity<PatientResponse> createPatient(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreatePatientRequest request
    ) {
        PatientResponse response = patientService.createPatient(new CreatePatientCommand(
                currentUser.tenantId(),
                currentUser.userId(),
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.sex(),
                request.phone(),
                request.email()
        ));

        return ResponseEntity
                .created(URI.create("/api/v1/patients/" + response.id()))
                .body(response);
    }
    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients(
            @AuthenticationPrincipal CurrentUser currentUser
    ){
       return ResponseEntity.ok(patientService.getAllPatients(currentUser.tenantId()));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatient(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID patientId
    ){
        return  ResponseEntity.ok(patientService.getPatient(currentUser.tenantId(), patientId));
    }
}
