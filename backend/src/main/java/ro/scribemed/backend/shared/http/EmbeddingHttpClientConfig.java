package ro.scribemed.backend.shared.http;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EmbeddingHttpClientConfig {

    @Bean
    @Qualifier("embeddingRestClient")
    RestClient embeddingRestClient(
            @Value("${scribemed.embedding.base-url}") String baseUrl
    ) {
        return Http11RestClientBuilder.build()
                .baseUrl(baseUrl)
                .build();
    }
}
