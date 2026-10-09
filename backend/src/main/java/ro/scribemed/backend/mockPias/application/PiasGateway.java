package ro.scribemed.backend.mockPias.application;

import org.springframework.stereotype.Component;
import ro.scribemed.backend.mockPias.domain.PiasValidationResult;
import ro.scribemed.backend.mockPias.infrastructure.messaging.PiasValidationRequestMessage;

public interface PiasGateway {
   PiasValidationResult validate(PiasValidationRequestMessage message);
}
