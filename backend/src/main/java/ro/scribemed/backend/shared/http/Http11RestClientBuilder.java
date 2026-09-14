package ro.scribemed.backend.shared.http;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

public final class Http11RestClientBuilder {

    private Http11RestClientBuilder() {
    }

    public static RestClient.Builder build() {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        return RestClient.builder()
                .requestFactory(
                        new JdkClientHttpRequestFactory(httpClient)
                );
    }
}
