package ro.scribemed.backend.patient.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.domain.PatientSex;
import ro.scribemed.backend.patient.domain.PatientStatus;

public record PatientResponse(
        UUID id,
        UUID tenantId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        PatientSex sex,
        String phone,
        String email,
        PatientStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getTenant().getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getBirthDate(),
                patient.getSex(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getStatus(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }
}
