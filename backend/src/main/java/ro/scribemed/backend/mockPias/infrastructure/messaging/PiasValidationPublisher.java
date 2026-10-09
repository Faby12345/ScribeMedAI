package ro.scribemed.backend.mockPias.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ro.scribemed.backend.mockPias.application.PiasValidationService;

@Component
public class PiasValidationPublisher {
    private final KafkaTemplate<String, PiasValidationRequestMessage> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(PiasValidationPublisher.class);


    public PiasValidationPublisher(
            KafkaTemplate<String, PiasValidationRequestMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void publish(PiasValidationRequestMessage message){
        kafkaTemplate.send(
                PiasTopics.VALIDATION_REQUESTED,
                message.jobId().toString(),
                message
        );
        log.info("Message produced: {}", message);
    }

}