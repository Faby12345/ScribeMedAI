package ro.scribemed.backend.mockPias.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import ro.scribemed.backend.identity.application.AuthService;
import ro.scribemed.backend.mockPias.application.PiasGateway;
import ro.scribemed.backend.mockPias.application.PiasValidationService;
import ro.scribemed.backend.mockPias.domain.PiasValidationResult;

@Component
public class PiasValidationConsumer {

    private final PiasValidationService validationService;
    private static final Logger log = LoggerFactory.getLogger(PiasValidationService.class);

    public PiasValidationConsumer(
            PiasValidationService validationService
    ) {
        this.validationService = validationService;
    }
    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 5_000, multiplier = 3),
            dltTopicSuffix = "-dlt"
    )

    @KafkaListener(
            topics = PiasTopics.VALIDATION_REQUESTED,
            groupId = "pias-integration"
    )
    public void consume(PiasValidationRequestMessage message) {
        PiasValidationResult result =
                validationService.processValidation(message);

        log.info("MESSAGE CONSUMED!: {} ", result.toString());
        // TODO: process the result
    }
}
