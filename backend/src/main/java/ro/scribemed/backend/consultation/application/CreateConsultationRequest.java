package ro.scribemed.backend.consultation.application;

import java.util.UUID;

public record CreateConsultationRequest(
        UUID tenantId,
        UUID actorUserId,
        UUID patientId
) {
}
