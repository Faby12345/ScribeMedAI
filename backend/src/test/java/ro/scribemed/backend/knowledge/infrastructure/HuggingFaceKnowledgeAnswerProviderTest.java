package ro.scribemed.backend.knowledge.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ro.scribemed.backend.knowledge.application.KnowledgeAnswerProvider;
import ro.scribemed.backend.knowledge.application.KnowledgeAnswerProviderException;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HuggingFaceKnowledgeAnswerProviderTest {

    private static final String BASE_URL = "https://huggingface.test/v1";

    @Test
    void generatesGroundedAnswerFromProvidedContexts() {
        TestFixture fixture = fixture("test-token");
        fixture.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.temperature").value(0.0))
                .andExpect(jsonPath("$.messages[0].content").value(
                        containsString("Folosește exclusiv")
                ))
                .andExpect(jsonPath("$.messages[1].content").value(
                        containsString("Care este recomandarea?")
                ))
                .andExpect(jsonPath("$.messages[1].content").value(
                        containsString("Fragment din ghid")
                ))
                .andRespond(withSuccess("""
                        {
                          "model": "test-model",
                          "choices": [{
                            "message": {
                              "role": "assistant",
                              "content": "Răspuns bazat pe ghid [1]."
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        KnowledgeAnswerProvider.KnowledgeAnswer answer = fixture.provider().generateAnswer(
                "  Care este recomandarea?  ",
                List.of(context())
        );

        assertEquals("Răspuns bazat pe ghid [1].", answer.answer());
        fixture.server().verify();
    }

    @Test
    void rejectsEmptyProviderResponse() {
        TestFixture fixture = fixture("test-token");
        fixture.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"\"}}]}",
                        MediaType.APPLICATION_JSON
                ));

        KnowledgeAnswerProviderException error = assertThrows(
                KnowledgeAnswerProviderException.class,
                () -> fixture.provider().generateAnswer("Întrebare", List.of(context()))
        );

        assertEquals("HUGGINGFACE_EMPTY_RESPONSE", error.getSafeErrorCode());
        fixture.server().verify();
    }

    @Test
    void sanitizesProviderHttpFailure() {
        TestFixture fixture = fixture("test-token");
        fixture.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .body("raw provider secret error"));

        KnowledgeAnswerProviderException error = assertThrows(
                KnowledgeAnswerProviderException.class,
                () -> fixture.provider().generateAnswer("Întrebare", List.of(context()))
        );

        assertEquals("HUGGINGFACE_REQUEST_FAILED", error.getSafeErrorCode());
        assertEquals("Knowledge answer generation request failed", error.getMessage());
        fixture.server().verify();
    }

    @Test
    void failsBeforeHttpCallWhenApiKeyIsMissing() {
        TestFixture fixture = fixture(" ");

        KnowledgeAnswerProviderException error = assertThrows(
                KnowledgeAnswerProviderException.class,
                () -> fixture.provider().generateAnswer("Întrebare", List.of(context()))
        );

        assertEquals("HUGGINGFACE_API_KEY_MISSING", error.getSafeErrorCode());
        fixture.server().verify();
    }

    private TestFixture fixture(String apiKey) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HuggingFaceKnowledgeAnswerProvider provider =
                new HuggingFaceKnowledgeAnswerProvider(
                        builder,
                        new ObjectMapper(),
                        apiKey,
                        BASE_URL,
                        "test-model"
                );
        return new TestFixture(provider, server);
    }

    private KnowledgeAnswerProvider.KnowledgeContext context() {
        return new KnowledgeAnswerProvider.KnowledgeContext(
                1,
                "Ghid clinic",
                10,
                11,
                "Tratament",
                "Fragment din ghid"
        );
    }

    private record TestFixture(
            HuggingFaceKnowledgeAnswerProvider provider,
            MockRestServiceServer server
    ) {
    }
}
