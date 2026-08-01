package ro.scribemed.backend.patient.application;

import java.time.LocalDate;
import java.util.UUID;

import ro.scribemed.backend.patient.domain.PatientSex;

public record CreatePatientCommand(
        UUID tenantId,
        UUID actorUserId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        PatientSex sex,
        String phone,
        String email
) {
}
