package ro.scribemed.backend.transcription.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ro.scribemed.backend.transcription.application.TranscriptionProvider;
import ro.scribemed.backend.transcription.application.TranscriptionProviderException;
import ro.scribemed.backend.transcription.dto.TranscriptionRequest;
import ro.scribemed.backend.transcription.application.TranscriptionResult;
import ro.scribemed.backend.transcription.config.DeepgramTranscriptionProperties;

@Service
public class DeepgramTranscriptionProvider implements TranscriptionProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DeepgramTranscriptionProperties properties;

    public DeepgramTranscriptionProvider(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            DeepgramTranscriptionProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public TranscriptionResult transcribe(TranscriptionRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new TranscriptionProviderException(
                    "DEEPGRAM_API_KEY_MISSING",
                    "Deepgram API key is not configured"
            );
        }

        String url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/v1/listen")
                .queryParam("model", properties.getModel())
                .queryParam("language", properties.getLanguage())
                .queryParam("smart_format", properties.isSmartFormat())
                .build()
                .toUriString();

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Token " + properties.getApiKey())
                    .contentType(MediaType.parseMediaType(request.contentType()))
                    .body(request.audioData())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException error) {
            throw new TranscriptionProviderException(
                    "DEEPGRAM_REQUEST_FAILED",
                    "Deepgram transcription request failed"
            );
        }

        return parseResponse(responseBody);
    }

    private TranscriptionResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode alternative = root.path("results")
                    .path("channels")
                    .path(0)
                    .path("alternatives")
                    .path(0);
            String transcript = alternative.path("transcript").asText("");

            if (transcript.isBlank()) {
                throw new TranscriptionProviderException(
                        "DEEPGRAM_EMPTY_TRANSCRIPT",
                        "Deepgram returned an empty transcript"
                );
            }

            return new TranscriptionResult(
                    "deepgram",
                    properties.getModel(),
                    properties.getLanguage(),
                    transcript,
                    responseBody
            );
        } catch (TranscriptionProviderException error) {
            throw error;
        } catch (Exception error) {
            throw new TranscriptionProviderException(
                    "DEEPGRAM_RESPONSE_INVALID",
                    "Deepgram returned an invalid response"
            );
        }
    }
}
