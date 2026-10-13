package ro.scribemed.backend.mockPias.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.mockPias.application.PiasValidationService;
import ro.scribemed.backend.mockPias.domain.PiasValidationResult;
import ro.scribemed.backend.mockPias.infrastructure.ProcessedJobRepository;

import java.time.Instant;
import java.util.UUID;

@Component
public class PiasValidationConsumer {

    private final PiasValidationService validationService;
    private final ProcessedJobRepository processedJobRepository;
    private static final Logger log = LoggerFactory.getLogger(PiasValidationConsumer.class);

    public PiasValidationConsumer(
            PiasValidationService validationService,
            ProcessedJobRepository processedJobRepository
    ) {
        this.validationService = validationService;
        this.processedJobRepository = processedJobRepository;
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 5_000, multiplier = 3),
            dltTopicSuffix = "-dlt"
    )

    @KafkaListener(
            topics = PiasTopics.VALIDATION_REQUESTED,
            groupId = PiasTopics.VALIDATION_CONSUMER_GROUP
    )
    @Transactional
    public void consume(PiasValidationRequestMessage message) {
        int claimed = processedJobRepository.insertIfAbsent(
                UUID.randomUUID(),
                message.tenantId(),
                message.jobId(),
                PiasTopics.VALIDATION_CONSUMER_GROUP,
                message.idempotencyKey(),
                Instant.now()
        );

        if (claimed == 0) {
            log.info(
                    "pias_validation_duplicate_skipped tenantId={} jobId={}",
                    message.tenantId(),
                    message.jobId()
            );
            return;
        }

        PiasValidationResult result =
                validationService.processValidation(message);

        log.info(
                "pias_validation_consumed tenantId={} jobId={} status={}",
                message.tenantId(),
                message.jobId(),
                result.status()
        );
        // TODO: process the result
    }
}
