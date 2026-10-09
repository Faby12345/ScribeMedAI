package ro.scribemed.backend.mockPias.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public record PiasValidationRequestMessage(
        UUID jobId,
        UUID tenantId,
        UUID consultationId,
        String idempotencyKey,
        Instant requestedAt,
        int schemaVersion
) {
}
