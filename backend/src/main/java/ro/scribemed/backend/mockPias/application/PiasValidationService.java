package ro.scribemed.backend.mockPias.application;

import org.springframework.stereotype.Service;
import ro.scribemed.backend.mockPias.domain.PiasValidationResult;
import ro.scribemed.backend.mockPias.infrastructure.messaging.PiasValidationPublisher;
import ro.scribemed.backend.mockPias.infrastructure.messaging.PiasValidationRequestMessage;

import java.time.Instant;
import java.util.UUID;

@Service
public class PiasValidationService {
    private final PiasValidationPublisher publisher;
    private final PiasGateway gateway;

    public PiasValidationService(
            PiasValidationPublisher publisher,
            PiasGateway gateway
    ) {
        this.publisher = publisher;
        this.gateway = gateway;
    }
    public void requestValidation(
            UUID tenantId,
            UUID consultationId
    ) {
        UUID jobId = UUID.randomUUID();

        PiasValidationRequestMessage message =
                new PiasValidationRequestMessage(
                        jobId,
                        tenantId,
                        consultationId,
                        jobId.toString(),
                        Instant.now(),
                        1
                );

        publisher.publish(message);
    }
    public PiasValidationResult processValidation(
            PiasValidationRequestMessage message
    ) {
        return gateway.validate(message);
    }

}
