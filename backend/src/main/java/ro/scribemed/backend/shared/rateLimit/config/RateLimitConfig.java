package ro.scribemed.backend.shared.rateLimit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.scribemed.backend.shared.rateLimit.domain.RateLimitPolicy;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    RateLimitPolicy loginRateLimitPolicy() {
        return new RateLimitPolicy(
                5,
                Duration.ofMinutes(15)

        );
    }

    @Bean
    RateLimitPolicy uploadAudioRateLimitPolicy() {
        return new RateLimitPolicy(
                10,
                Duration.ofMinutes(60)
        );
    }

    @Bean
    RateLimitPolicy aiGenerationTenantRateLimitPolicy() {
        return new RateLimitPolicy(
                30,
                Duration.ofDays(1)
        );
    }

    @Bean
    RateLimitPolicy patientReadsRateLimitPolicy() {
        return new RateLimitPolicy(
                120,
                Duration.ofMinutes(1)
        );
    }
}
