package ro.scribemed.backend.knowledge.infrastructure.embedding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.scribemed.backend.knowledge.application.EmbeddingProvider;
import ro.scribemed.backend.knowledge.infrastructure.embedding.dto.EmbeddingRequest;
import ro.scribemed.backend.knowledge.infrastructure.embedding.dto.EmbeddingResponse;

import java.util.List;

@Component
public class FastApiEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final int expectedDimensions;


    public FastApiEmbeddingProvider(
            @Qualifier("embeddingRestClient") RestClient restClient,
            @Value("${scribemed.embedding.expected-dimensions}")
            int expectedDimensions
    ) {
        this.restClient = restClient;
        this.expectedDimensions = expectedDimensions;
    }



    @Override
    public List<Double> embed(String text) {

        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new EmbeddingRequest(List.of(text)))
                .retrieve()
                .body(EmbeddingResponse.class);


        if(response == null || response.embeddings().size() != 1){
            throw new IllegalStateException(
                    "Invalid embedding service response"
            );
        }

        List<Double> embedding = response.embeddings().getFirst();


        if(embedding.size() != expectedDimensions) {
            throw new IllegalStateException(
                    "Expected %d dimensions, received %d"
                            .formatted(
                                    expectedDimensions,
                                    embedding.size()
                            )
            );
        }

        return embedding;
    }
}
