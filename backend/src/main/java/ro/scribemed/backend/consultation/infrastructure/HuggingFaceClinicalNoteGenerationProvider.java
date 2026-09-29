package ro.scribemed.backend.consultation.infrastructure;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationProvider;
import ro.scribemed.backend.consultation.application.exception.ClinicalNoteGenerationProviderException;
import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationResult;
import ro.scribemed.backend.consultation.infrastructure.dto.HuggingFaceChatRequest;
import ro.scribemed.backend.consultation.infrastructure.dto.HuggingFaceChatResponse;
import ro.scribemed.backend.consultation.dto.NotesRequest;
import ro.scribemed.backend.consultation.dto.TranscriptNoteGenerationRequest;

@Component
public class HuggingFaceClinicalNoteGenerationProvider implements ClinicalNoteGenerationProvider {

    private static final String PROVIDER = "huggingface";
    private static final String MODEL = "Qwen/Qwen3-32B:cheapest";
    private static final String DOCTOR_NOTES_PROMPT_VERSION = "clinical-note-soap-from-notes-v1";
    private static final String TRANSCRIPT_PROMPT_VERSION = "clinical-note-soap-from-transcript-v1";
    private static final String TEMPLATE_VERSION = "soap-v1";
    private static final double TEMPERATURE = 0.0;
    private static final int MAX_TOKENS_TEST_CONNECTION = 5;
    private static final int MAX_TOKENS_NOTES = 1000;
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceClinicalNoteGenerationProvider.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceClinicalNoteGenerationProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${scribemed.llm.huggingface.api-key}") String apiKey
    ) {
        this.objectMapper = objectMapper;

        this.restClient = builder
                .baseUrl("https://router.huggingface.co/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public boolean isAvailable() {
        try {
            HuggingFaceChatRequest request = new HuggingFaceChatRequest(
                    MODEL,
                    List.of(
                            new HuggingFaceChatRequest.Message(
                                    "system",
                                    "You are a clinical documentation assistant."
                            ),
                            new HuggingFaceChatRequest.Message(
                                    "user",
                                    "Reply exactly with OK"
                            )
                    ),
                    TEMPERATURE,
                    MAX_TOKENS_TEST_CONNECTION
            );
            HuggingFaceChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(HuggingFaceChatResponse.class);

            return response != null
                    && response.choices() != null
                    && !response.choices().isEmpty();
        } catch (RestClientException error) {
            throw new ClinicalNoteGenerationProviderException(
                    "HUGGINGFACE_UNAVAILABLE",
                    "LLM model is not available"
            );
        }
    }

    @Override
    public ClinicalNoteGenerationResult generateFromDoctorNotes(NotesRequest rawNotes) {
        return generate(
                doctorNotesSystemPrompt(),
                doctorNotesUserPrompt(rawNotes),
                DOCTOR_NOTES_PROMPT_VERSION
        );
    }

    @Override
    public ClinicalNoteGenerationResult generateFromTranscript(TranscriptNoteGenerationRequest transcript) {
        return generate(
                transcriptSystemPrompt(),
                transcriptUserPrompt(transcript),
                TRANSCRIPT_PROMPT_VERSION
        );
    }

    private ClinicalNoteGenerationResult generate(String systemPrompt, String userPrompt, String promptVersion) {
        HuggingFaceChatRequest request = new HuggingFaceChatRequest(
                MODEL,
                List.of(
                        new HuggingFaceChatRequest.Message("system", systemPrompt),
                        new HuggingFaceChatRequest.Message("user", userPrompt)
                ),
                TEMPERATURE,
                MAX_TOKENS_NOTES
        );

        String responseBody;

        try {
            responseBody = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException error) {

            log.error(
                    "clinical_note_generation_provider_request_failed provider={} status={}",
                    PROVIDER,
                    error.getStatusCode()
            );

            throw new ClinicalNoteGenerationProviderException(
                    "HUGGINGFACE_REQUEST_FAILED",
                    "Clinical note generation request failed"
            );

        } catch (RestClientException error) {

            log.error(
                    "clinical_note_generation_provider_communication_failed provider={} errorType={}",
                    PROVIDER,
                    error.getClass().getSimpleName()
            );

            throw new ClinicalNoteGenerationProviderException(
                    "HUGGINGFACE_REQUEST_FAILED",
                    "Clinical note generation request failed"
            );
        }

        return parseResponse(responseBody, promptVersion);
    }

    private ClinicalNoteGenerationResult parseResponse(String responseBody, String promptVersion) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");

            if (content.isBlank()) {
                throw new ClinicalNoteGenerationProviderException(
                        "HUGGINGFACE_EMPTY_RESPONSE",
                        "Clinical note generation returned an empty response"
                );
            }

            JsonNode generatedNote = objectMapper.readTree(stripJsonFence(content));
            ClinicalNoteGenerationResult.SoapNote soapNote = new ClinicalNoteGenerationResult.SoapNote(
                    requiredText(generatedNote, "subjective"),
                    requiredText(generatedNote, "objective"),
                    requiredText(generatedNote, "assessment"),
                    requiredText(generatedNote, "plan")
            );
            List<String> reviewFlags = parseReviewFlags(generatedNote);

            return new ClinicalNoteGenerationResult(
                    PROVIDER,
                    root.path("model").asText(MODEL),
                    promptVersion,
                    TEMPLATE_VERSION,
                    soapNote,
                    reviewFlags,
                    responseBody
            );
        } catch (ClinicalNoteGenerationProviderException error) {
            throw error;
        } catch (Exception error) {
            throw new ClinicalNoteGenerationProviderException(
                    "HUGGINGFACE_RESPONSE_INVALID",
                    "Clinical note generation returned an invalid response"
            );
        }
    }

    private String doctorNotesSystemPrompt() {
        return """
                Ești un asistent pentru documentație medicală în limba română.
                Transformă notițele brute ale medicului într-un draft clinic structurat SOAP.
                Nu inventa diagnostice, simptome, tratamente, rezultate de examen obiectiv sau recomandări care nu apar în notițe.
                Dacă o secțiune nu are suficiente informații, scrie explicit că informația nu este documentată în notițele primite.
                Răspunde exclusiv cu JSON valid, fără Markdown și fără text în afara JSON-ului.
                Schema obligatorie:
                {
                  "subjective": "string",
                  "objective": "string",
                  "assessment": "string",
                  "plan": "string",
                  "reviewFlags": ["string"]
                }
                """;
    }

    private String transcriptSystemPrompt() {
        return """
                Ești un asistent pentru documentație medicală în limba română.
                Transformă transcrierea unei consultații medicale într-un draft clinic structurat SOAP.
                Transcrierea poate conține dialog, repetiții, erori de recunoaștere vocală și informații incomplete.
                Nu inventa diagnostice, simptome, tratamente, rezultate de examen obiectiv sau recomandări care nu apar în transcriere.
                Dacă o secțiune nu are suficiente informații, scrie explicit că informația nu este documentată în transcriere.
                Marchează pentru verificare afirmațiile clinice incerte, medicamentele, dozele, alergiile, valorile numerice și posibilele erori de negație.
                Răspunde exclusiv cu JSON valid, fără Markdown și fără text în afara JSON-ului.
                Schema obligatorie:
                {
                  "subjective": "string",
                  "objective": "string",
                  "assessment": "string",
                  "plan": "string",
                  "reviewFlags": ["string"]
                }
                """;
    }

    private String doctorNotesUserPrompt(NotesRequest rawNotes) {
        return """
                Generează un draft SOAP pe baza următoarelor notițe brute.

                Motivul prezentării:
                %s

                Istoric / anamneză:
                %s

                Obiectiv:
                %s

                Evaluare:
                %s

                Plan:
                %s
                """.formatted(
                safe(rawNotes.reason()),
                safe(rawNotes.history()),
                safe(rawNotes.objective()),
                safe(rawNotes.assessment()),
                safe(rawNotes.plan())
        );
    }

    private String transcriptUserPrompt(TranscriptNoteGenerationRequest transcript) {
        return """
                Generează un draft SOAP pe baza următoarei transcrieri de consultație.

                Limba transcrierii:
                %s

                Furnizor transcriere:
                %s

                Model transcriere:
                %s

                Transcriere:
                %s
                """.formatted(
                safe(transcript.language()),
                safe(transcript.transcriptionProvider()),
                safe(transcript.transcriptionModel()),
                safe(transcript.transcriptText())
        );
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("");
        if (value.isBlank()) {
            throw new ClinicalNoteGenerationProviderException(
                    "HUGGINGFACE_RESPONSE_SCHEMA_INVALID",
                    "Clinical note generation response does not match the expected schema"
            );
        }
        return value;
    }

    private List<String> parseReviewFlags(JsonNode generatedNote) {
        JsonNode reviewFlags = generatedNote.path("reviewFlags");
        if (!reviewFlags.isArray()) {
            return List.of();
        }

        return objectMapper.convertValue(
                reviewFlags,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "Nedocumentat.";
        }
        return value.strip();
    }

    private String stripJsonFence(String content) {
        String stripped = content.strip();
        if (stripped.startsWith("```json") && stripped.endsWith("```")) {
            return stripped.substring(7, stripped.length() - 3).strip();
        }
        if (stripped.startsWith("```") && stripped.endsWith("```")) {
            return stripped.substring(3, stripped.length() - 3).strip();
        }
        return stripped;
    }
}
