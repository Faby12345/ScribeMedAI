package ro.scribemed.backend.mockPias.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ro.scribemed.backend.mockPias.application.PiasGateway;
import ro.scribemed.backend.mockPias.domain.PiasValidationResult;
import ro.scribemed.backend.mockPias.domain.PiasValidationStatus;
import ro.scribemed.backend.mockPias.infrastructure.messaging.PiasValidationRequestMessage;
import ro.scribemed.backend.shared.http.Http11RestClientBuilder;

@Component
public class HttpPiasGateway implements PiasGateway {

    private final RestClient restClient;

    public HttpPiasGateway(
            @Value("${scribemed.mock-pias.base-url}") String baseUrl) {
        this.restClient = Http11RestClientBuilder.build()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public PiasValidationResult validate(PiasValidationRequestMessage message) {

        try {
            MockPiasResponse response = restClient.post()
                    .uri("/pias/submissions")
                    .body(new MockPiasRequest(
                             message.jobId(),
                             message.consultationId(),
                             message.idempotencyKey()
                            )
                    )
                    .retrieve()
                    .body(MockPiasResponse.class);
            if(response == null){
                throw new RuntimeException("PIAS returned an empty response");
            }
            if (!"accepted".equals(response.status())) {
                throw new RuntimeException("PIAS returned an unexpected status");
            }
            return new PiasValidationResult(
                    PiasValidationStatus.VALID,
                    response.reference(),
                    "Validarea PIAS a fost acceptată."
            );

        } catch (RestClientResponseException exception ){
            if (exception.getStatusCode().is4xxClientError()) {
                return new PiasValidationResult(
                        PiasValidationStatus.INVALID,
                        "PIAS_REQUEST_REJECTED",
                        "Cererea de validare a fost respinsă."
                );
            }
            throw new RuntimeException(
                    "Mock PIAS returned a temporary server error",
                    exception
            );
        } catch (RestClientException exception){
            throw new RuntimeException(
                    "Mock PIAS could not be reached!"
            );
        }
    }
    private record MockPiasRequest(
            java.util.UUID jobId,
            java.util.UUID consultationId,
            String idempotencyKey
    ) {}

    private record MockPiasResponse(
            String status,
            String reference
    ) {}
}
