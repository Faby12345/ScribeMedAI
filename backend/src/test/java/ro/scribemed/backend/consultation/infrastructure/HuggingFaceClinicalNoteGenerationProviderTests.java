package ro.scribemed.backend.consultation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import ro.scribemed.backend.consultation.application.ClinicalNoteGenerationResult;
import ro.scribemed.backend.consultation.application.exception.ClinicalNoteGenerationProviderException;
import ro.scribemed.backend.medicationdraft.domain.MedicationMentionType;

class HuggingFaceClinicalNoteGenerationProviderTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HuggingFaceClinicalNoteGenerationProvider provider =
            new HuggingFaceClinicalNoteGenerationProvider(
                    RestClient.builder(),
                    objectMapper,
                    "test-api-key"
            );

    @Test
    void parsesMedicationCandidatesFromTranscriptResponse() throws Exception {
        ObjectNode generatedNote = soapNote();
        ObjectNode medication = generatedNote.putArray("medications").addObject();
        medication.put("mentionType", "START");
        medication.put("extractedName", " Paracetamol ");
        medication.put("dose", " 500 mg ");
        medication.putNull("administrationRoute");
        medication.put("frequency", "de două ori pe zi");
        medication.put("duration", "5 zile");
        medication.putNull("quantity");
        medication.put("instructions", "după masă");
        medication.putArray("sourceSegmentIds");
        generatedNote.putArray("reviewFlags");

        ClinicalNoteGenerationResult result = provider.parseResponse(
                providerResponse(generatedNote),
                "prompt-v2",
                "template-v1",
                true
        );

        assertThat(result.medications()).hasSize(1);
        ClinicalNoteGenerationResult.MedicationCandidate candidate = result.medications().getFirst();
        assertThat(candidate.mentionType()).isEqualTo(MedicationMentionType.START);
        assertThat(candidate.extractedName()).isEqualTo("Paracetamol");
        assertThat(candidate.dose()).isEqualTo("500 mg");
        assertThat(candidate.administrationRoute()).isNull();
        assertThat(candidate.sourceSegmentIds()).isEmpty();
        assertThat(result.reviewFlags()).containsExactly("MEDICATION_REVIEW");
    }

    @Test
    void rejectsInventedSourceSegmentIdentifiersUntilSegmentsAreSupported() throws Exception {
        ObjectNode generatedNote = soapNote();
        ObjectNode medication = generatedNote.putArray("medications").addObject();
        medication.put("mentionType", "CURRENT");
        medication.put("extractedName", "Ibuprofen");
        medication.putNull("dose");
        medication.putNull("administrationRoute");
        medication.putNull("frequency");
        medication.putNull("duration");
        medication.putNull("quantity");
        medication.putNull("instructions");
        medication.putArray("sourceSegmentIds").add("seg-inventat");
        generatedNote.putArray("reviewFlags");

        assertThatThrownBy(() -> provider.parseResponse(
                providerResponse(generatedNote),
                "prompt-v2",
                "template-v1",
                true
        )).isInstanceOf(ClinicalNoteGenerationProviderException.class);
    }

    @Test
    void requiresMedicationArrayForTranscriptResponses() throws Exception {
        ObjectNode generatedNote = soapNote();
        generatedNote.putArray("reviewFlags");

        assertThatThrownBy(() -> provider.parseResponse(
                providerResponse(generatedNote),
                "prompt-v2",
                "template-v1",
                true
        )).isInstanceOf(ClinicalNoteGenerationProviderException.class);
    }

    private ObjectNode soapNote() {
        ObjectNode note = objectMapper.createObjectNode();
        note.put("subjective", "Subiectiv");
        note.put("objective", "Obiectiv");
        note.put("assessment", "Evaluare");
        note.put("plan", "Plan");
        return note;
    }

    private String providerResponse(ObjectNode generatedNote) throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("model", "test-model");
        ArrayNode choices = response.putArray("choices");
        choices.addObject()
                .putObject("message")
                .put("content", objectMapper.writeValueAsString(generatedNote));
        return objectMapper.writeValueAsString(response);
    }
}
