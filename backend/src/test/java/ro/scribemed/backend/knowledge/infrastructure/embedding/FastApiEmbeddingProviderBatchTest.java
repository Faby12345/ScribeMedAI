package ro.scribemed.backend.knowledge.infrastructure.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiEmbeddingProviderBatchTest {

    @Test
    void splitsRequestsAndPreservesResponseOrder() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8000/embeddings"))
                .andExpect(jsonPath("$.texts.length()").value(32))
                .andRespond(withSuccess(response(32, 1.0), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8000/embeddings"))
                .andExpect(jsonPath("$.texts.length()").value(1))
                .andRespond(withSuccess(response(1, 2.0), MediaType.APPLICATION_JSON));

        FastApiEmbeddingProvider provider = new FastApiEmbeddingProvider(builder.build(), 2);
        List<List<Double>> embeddings = provider.embedAll(
                IntStream.range(0, 33).mapToObj(index -> "passage: " + index).toList()
        );

        assertEquals(33, embeddings.size());
        assertEquals(List.of(1.0, 0.0), embeddings.get(31));
        assertEquals(List.of(2.0, 0.0), embeddings.get(32));
        server.verify();
    }

    @Test
    void rejectsMissingEmbeddings() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8000/embeddings"))
                .andRespond(withSuccess(response(1, 1.0), MediaType.APPLICATION_JSON));

        FastApiEmbeddingProvider provider = new FastApiEmbeddingProvider(builder.build(), 2);
        assertThrows(IllegalStateException.class,
                () -> provider.embedAll(List.of("passage: one", "passage: two")));
        server.verify();
    }

    private String response(int count, double firstValue) {
        String vectors = IntStream.range(0, count)
                .mapToObj(index -> "[" + firstValue + ",0.0]")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return "{\"model\":\"test\",\"dimensions\":2,\"embeddings\":[" + vectors + "]}";
    }
}
