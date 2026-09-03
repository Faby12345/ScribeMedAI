package ro.scribemed.backend.shared.rateLimit.infrastructure.http;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ro.scribemed.backend.shared.rateLimit.domain.*;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private final RateLimiter rateLimiter = mock(RateLimiter.class);

    private final RateLimitPolicy loginPolicy = new RateLimitPolicy(
            5,
            Duration.ofMinutes(15)
    );

    private final RateLimitFilter filter = new RateLimitFilter(
            rateLimiter,
            loginPolicy
    );


    @Test
    void blockLoginWhenLimitIsExceeded() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/auth/login"
        );

        request.setRemoteAddr("203.0.113.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        Instant resetAt = Instant.now().plusSeconds(60);

        when(rateLimiter.tryAcquire(
                eq(new RateLimitKey(RateLimitScope.LOGIN_BY_IP, "203.0.113.10")),
                eq(loginPolicy)
        )).thenReturn(
                new RateLimitDecision(
                        false,
                        5,
                        0,
                        resetAt
                )
        );

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        assertThat(response.getHeader("X-RateLimit-Limit"))
                .isEqualTo("5");

        assertThat(response.getHeader("X-RateLimit-Remaining"))
                .isEqualTo("0");

        assertThat(response.getHeader("Retry-After"))
                .isNotBlank();

        verify(rateLimiter).tryAcquire(
                new RateLimitKey(RateLimitScope.LOGIN_BY_IP, "203.0.113.10"),
                loginPolicy
        );

        verifyNoInteractions(filterChain);


    }
}