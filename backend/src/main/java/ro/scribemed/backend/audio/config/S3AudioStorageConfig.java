package ro.scribemed.backend.audio.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
@EnableConfigurationProperties({AudioStorageProperties.class, S3AudioStorageProperties.class})
class S3AudioStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "scribemed.audio.storage", name = "backend", havingValue = "s3")
    S3Client s3Client(S3AudioStorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
            builder.region(Region.of(properties.getRegion()));
        }
        if (properties.getEndpointOverride() != null) {
            builder.endpointOverride(properties.getEndpointOverride());
        }
        if (properties.isPathStyleAccessEnabled()) {
            builder.forcePathStyle(true);
        }

        return builder.build();
    }
}
