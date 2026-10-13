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
import ro.scribemed.backend.medicationdraft.domain.MedicationMentionType;

@Component
public class HuggingFaceClinicalNoteGenerationProvider implements ClinicalNoteGenerationProvider {

    private static final String PROVIDER = "huggingface";
    private static final String MODEL = "Qwen/Qwen3-32B:cheapest";
    private static final String DOCTOR_NOTES_PROMPT_VERSION = "clinical-note-soap-from-notes-v1";
    private static final String TRANSCRIPT_PROMPT_VERSION = "clinical-note-soap-medications-from-transcript-v2";
    private static final String SOAP_TEMPLATE_VERSION = "soap-v1";
    private static final String TRANSCRIPT_TEMPLATE_VERSION = "soap-medications-v1";
    private static final double TEMPERATURE = 0.0;
    private static final int MAX_TOKENS_TEST_CONNECTION = 5;
    private static final int MAX_TOKENS_NOTES = 1600;
    private static final int MAX_MEDICATION_CANDIDATES = 20;
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
                DOCTOR_NOTES_PROMPT_VERSION,
                SOAP_TEMPLATE_VERSION,
                false
        );
    }

    @Override
    public ClinicalNoteGenerationResult generateFromTranscript(TranscriptNoteGenerationRequest transcript) {
        return generate(
                transcriptSystemPrompt(),
                transcriptUserPrompt(transcript),
                TRANSCRIPT_PROMPT_VERSION,
                TRANSCRIPT_TEMPLATE_VERSION,
                true
        );
    }

    private ClinicalNoteGenerationResult generate(
            String systemPrompt,
            String userPrompt,
            String promptVersion,
            String templateVersion,
            boolean requireMedications
    ) {
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

        return parseResponse(responseBody, promptVersion, templateVersion, requireMedications);
    }

    ClinicalNoteGenerationResult parseResponse(
            String responseBody,
            String promptVersion,
            String templateVersion,
            boolean requireMedications
    ) {
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
            List<ClinicalNoteGenerationResult.MedicationCandidate> medications =
                    parseMedicationCandidates(generatedNote, requireMedications);
            List<String> reviewFlags = parseReviewFlags(generatedNote);
            if (!medications.isEmpty() && !reviewFlags.contains("MEDICATION_REVIEW")) {
                reviewFlags = new java.util.ArrayList<>(reviewFlags);
                reviewFlags.add("MEDICATION_REVIEW");
            }

            return new ClinicalNoteGenerationResult(
                    PROVIDER,
                    root.path("model").asText(MODEL),
                    promptVersion,
                    templateVersion,
                    soapNote,
                    medications,
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
                Extrage separat numai medicamentele menționate explicit în transcriere.
                Pentru fiecare medicament, clasifică intenția ca CURRENT, START, CHANGE, STOP sau UNCERTAIN.
                Nu deduce și nu completa denumiri, doze, căi de administrare, frecvențe, durate, cantități sau instrucțiuni care nu apar explicit.
                Folosește null pentru orice atribut absent sau incert.
                Nu genera coduri CIM și nu transforma automat un medicament identificat într-o prescripție.
                Transcrierea nu conține încă identificatori canonici de segmente; returnează sourceSegmentIds ca listă goală și nu inventa identificatori.
                Răspunde exclusiv cu JSON valid, fără Markdown și fără text în afara JSON-ului.
                Schema obligatorie:
                {
                  "subjective": "string",
                  "objective": "string",
                  "assessment": "string",
                  "plan": "string",
                  "medications": [
                    {
                      "mentionType": "CURRENT | START | CHANGE | STOP | UNCERTAIN",
                      "extractedName": "string",
                      "dose": "string | null",
                      "administrationRoute": "string | null",
                      "frequency": "string | null",
                      "duration": "string | null",
                      "quantity": "string | null",
                      "instructions": "string | null",
                      "sourceSegmentIds": []
                    }
                  ],
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

    private List<ClinicalNoteGenerationResult.MedicationCandidate> parseMedicationCandidates(
            JsonNode generatedNote,
            boolean required
    ) {
        JsonNode medicationsNode = generatedNote.path("medications");
        if (!medicationsNode.isArray()) {
            if (!required && medicationsNode.isMissingNode()) {
                return List.of();
            }
            throw invalidSchema();
        }
        if (medicationsNode.size() > MAX_MEDICATION_CANDIDATES) {
            throw invalidSchema();
        }

        java.util.ArrayList<ClinicalNoteGenerationResult.MedicationCandidate> medications =
                new java.util.ArrayList<>(medicationsNode.size());
        for (JsonNode medicationNode : medicationsNode) {
            if (!medicationNode.isObject()) {
                throw invalidSchema();
            }

            MedicationMentionType mentionType;
            try {
                mentionType = MedicationMentionType.valueOf(requiredText(
                        medicationNode,
                        "mentionType",
                        30
                ));
            } catch (IllegalArgumentException error) {
                throw invalidSchema();
            }

            List<String> sourceSegmentIds = parseSourceSegmentIds(medicationNode);
            if (!sourceSegmentIds.isEmpty()) {
                throw invalidSchema();
            }

            medications.add(new ClinicalNoteGenerationResult.MedicationCandidate(
                    mentionType,
                    requiredText(medicationNode, "extractedName", 255),
                    optionalText(medicationNode, "dose", 255),
                    optionalText(medicationNode, "administrationRoute", 100),
                    optionalText(medicationNode, "frequency", 255),
                    optionalText(medicationNode, "duration", 255),
                    optionalText(medicationNode, "quantity", 255),
                    optionalText(medicationNode, "instructions", 4000),
                    sourceSegmentIds
            ));
        }
        return List.copyOf(medications);
    }

    private List<String> parseSourceSegmentIds(JsonNode medicationNode) {
        JsonNode sourceSegmentIds = medicationNode.path("sourceSegmentIds");
        if (!sourceSegmentIds.isArray()) {
            throw invalidSchema();
        }
        for (JsonNode sourceSegmentId : sourceSegmentIds) {
            if (!sourceSegmentId.isTextual() || sourceSegmentId.asText().isBlank()) {
                throw invalidSchema();
            }
        }
        return objectMapper.convertValue(
                sourceSegmentIds,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
    }

    private String requiredText(JsonNode node, String fieldName, int maxLength) {
        String value = requiredText(node, fieldName).strip();
        if (value.length() > maxLength) {
            throw invalidSchema();
        }
        return value;
    }

    private String optionalText(JsonNode node, String fieldName, int maxLength) {
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (!valueNode.isTextual()) {
            throw invalidSchema();
        }
        String value = valueNode.asText().strip();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > maxLength) {
            throw invalidSchema();
        }
        return value;
    }

    private ClinicalNoteGenerationProviderException invalidSchema() {
        return new ClinicalNoteGenerationProviderException(
                "HUGGINGFACE_RESPONSE_SCHEMA_INVALID",
                "Clinical note generation response does not match the expected schema"
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
