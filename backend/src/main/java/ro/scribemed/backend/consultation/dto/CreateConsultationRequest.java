package ro.scribemed.backend.consultation.dto;

import java.util.UUID;

public record CreateConsultationRequest(
        UUID tenantId,
        UUID actorUserId,
        UUID patientId
) {
}
