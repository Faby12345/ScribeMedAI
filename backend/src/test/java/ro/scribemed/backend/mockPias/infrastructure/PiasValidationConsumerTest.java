package ro.scribemed.backend.mockPias.infrastructure;

import org.junit.jupiter.api.Test;
import ro.scribemed.backend.mockPias.application.PiasValidationService;
import ro.scribemed.backend.mockPias.domain.PiasValidationResult;
import ro.scribemed.backend.mockPias.domain.PiasValidationStatus;
import ro.scribemed.backend.mockPias.infrastructure.messaging.PiasTopics;
import ro.scribemed.backend.mockPias.infrastructure.messaging.PiasValidationConsumer;
import ro.scribemed.backend.mockPias.infrastructure.messaging.PiasValidationRequestMessage;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PiasValidationConsumerTest {

    private final PiasValidationService validationService =
            mock(PiasValidationService.class);
    private final ProcessedJobRepository processedJobRepository =
            mock(ProcessedJobRepository.class);
    private final PiasValidationConsumer consumer =
            new PiasValidationConsumer(validationService, processedJobRepository);

    @Test
    void processesAJobAfterClaimingItsIdempotencyKey() {
        PiasValidationRequestMessage message = message();
        when(processedJobRepository.insertIfAbsent(
                any(UUID.class),
                eq(message.tenantId()),
                eq(message.jobId()),
                eq(PiasTopics.VALIDATION_CONSUMER_GROUP),
                eq(message.idempotencyKey()),
                any(Instant.class)
        )).thenReturn(1);
        when(validationService.processValidation(message)).thenReturn(
                new PiasValidationResult(
                        PiasValidationStatus.VALID,
                        "accepted",
                        "Validarea PIAS a fost acceptată."
                )
        );

        consumer.consume(message);

        verify(validationService).processValidation(message);
    }

    @Test
    void skipsAJobWhoseIdempotencyKeyWasAlreadyClaimed() {
        PiasValidationRequestMessage message = message();
        when(processedJobRepository.insertIfAbsent(
                any(UUID.class),
                eq(message.tenantId()),
                eq(message.jobId()),
                eq(PiasTopics.VALIDATION_CONSUMER_GROUP),
                eq(message.idempotencyKey()),
                any(Instant.class)
        )).thenReturn(0);

        consumer.consume(message);

        verify(validationService, never()).processValidation(any());
    }

    private PiasValidationRequestMessage message() {
        UUID jobId = UUID.randomUUID();
        return new PiasValidationRequestMessage(
                jobId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                jobId.toString(),
                Instant.parse("2026-10-10T10:00:00Z"),
                1
        );
    }
}
