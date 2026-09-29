package ro.scribemed.backend.knowledge.infrastructure.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiEmbeddingProviderTest {

    @Test
    void shouldGenerate384DimensionalEmbedding() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8000/embeddings"))
                .andExpect(jsonPath("$.texts.length()").value(1))
                .andExpect(jsonPath("$.texts[0]").value("query: Care este tratamentul schizofreniei?"))
                .andRespond(withSuccess(response(), MediaType.APPLICATION_JSON));
        FastApiEmbeddingProvider embeddingProvider = new FastApiEmbeddingProvider(builder.build(), 384);

        List<Double> embedding = embeddingProvider.embed("query: Care este tratamentul schizofreniei?");

        assertNotNull(embedding);
        assertEquals(384, embedding.size());
        server.verify();
    }

    private String response() {
        String vector = IntStream.range(0, 384)
                .mapToObj(index -> index == 0 ? "1.0" : "0.0")
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
        return "{\"model\":\"test\",\"dimensions\":384,\"embeddings\":[[" + vector + "]]}";
    }
}
