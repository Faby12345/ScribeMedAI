package ro.scribemed.backend.consultation.dto;

import java.time.Instant;
import java.util.UUID;

import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.consultation.domain.ConsultationStatus;

public record ConsultationResponse(
        UUID id,
        UUID tenantId,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        UUID doctorUserId,
        ConsultationStatus status,
        Instant patientInformedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static ConsultationResponse from(Consultation consultation) {
        return new ConsultationResponse(
                consultation.getId(),
                consultation.getTenant().getId(),
                consultation.getPatient().getId(),
                consultation.getPatient().getFirstName(),
                consultation.getPatient().getLastName(),
                consultation.getDoctorUser().getId(),
                consultation.getStatus(),
                consultation.getPatientInformedAt(),
                consultation.getCreatedAt(),
                consultation.getUpdatedAt()
        );
    }
}
