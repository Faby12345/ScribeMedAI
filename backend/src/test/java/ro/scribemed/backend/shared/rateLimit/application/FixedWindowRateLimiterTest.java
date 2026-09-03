package ro.scribemed.backend.shared.rateLimit.application;

import org.junit.jupiter.api.Test;
import ro.scribemed.backend.shared.rateLimit.domain.*;
import ro.scribemed.backend.shared.rateLimit.infrastructure.persistence.RateLimitWindowRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FixedWindowRateLimiterTest {

    private final RateLimitWindowRepository repository =
            mock(RateLimitWindowRepository.class);

    private final FixedWindowRateLimiter rateLimiter =
            new FixedWindowRateLimiter(repository);


    @Test
    void allowsFirstRequestAndLeavesFourRemaining(){

        RateLimitKey key = new RateLimitKey(
                RateLimitScope.LOGIN_BY_IP,
                "203.0.113.10"
        );

        RateLimitPolicy policy = new RateLimitPolicy(
                5,
                Duration.ofMinutes(15)
        );

        RateLimitWindow window = new RateLimitWindow(
                RateLimitScope.LOGIN_BY_IP,
                "203.0.113.10",
                Instant.now()
        );

        when(repository.findByScopeAndIdentifier(
                RateLimitScope.LOGIN_BY_IP,
                "203.0.113.10"
        )).thenReturn(Optional.of(window));

        RateLimitDecision decision = rateLimiter.tryAcquire(key, policy);

        assertThat(decision.allowed()).isTrue();

        assertThat(decision.remaining()).isEqualTo(4);

        assertThat(window.getRequestCount()).isEqualTo(1);
    }



}