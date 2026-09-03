package ro.scribemed.backend.shared.rateLimit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
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
}
