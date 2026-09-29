package ro.scribemed.backend.shared.rateLimit.infrastructure.http;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.shared.rateLimit.domain.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private final RateLimiter rateLimiter = mock(RateLimiter.class);

    private final RateLimitPolicy loginPolicy = new RateLimitPolicy(
            5,
            Duration.ofMinutes(15)
    );

    private final  RateLimitPolicy uploadAudioPolicy = new RateLimitPolicy(
            10,
            Duration.ofMinutes(60)
    );

    private final RateLimitPolicy aiGenerationTenantPolicy = new RateLimitPolicy(
            30,
            Duration.ofDays(1)
    );

    private final RateLimitPolicy patientReadsPolicy = new RateLimitPolicy(
            120,
            Duration.ofMinutes(1)
    );

    private final RateLimitFilter filter = new RateLimitFilter(
            rateLimiter,
            loginPolicy,
            uploadAudioPolicy,
            aiGenerationTenantPolicy,
            patientReadsPolicy
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


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

    @Test
    void appliesAudioUploadLimitByAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UUID.randomUUID());

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/consultations/" + UUID.randomUUID() + "/audio"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(rateLimiter.tryAcquire(
                eq(new RateLimitKey(RateLimitScope.AUDIO_UPLOAD_BY_USER, userId.toString())),
                eq(uploadAudioPolicy)
        )).thenReturn(allowedDecision(uploadAudioPolicy));

        filter.doFilter(request, response, filterChain);

        verify(rateLimiter).tryAcquire(
                new RateLimitKey(RateLimitScope.AUDIO_UPLOAD_BY_USER, userId.toString()),
                uploadAudioPolicy
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void appliesAiGenerationLimitByAuthenticatedTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticate(UUID.randomUUID(), tenantId);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/consultations/" + UUID.randomUUID() + "/notes"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(rateLimiter.tryAcquire(
                eq(new RateLimitKey(RateLimitScope.LLM_GENERATION_BY_TENANT, tenantId.toString())),
                eq(aiGenerationTenantPolicy)
        )).thenReturn(allowedDecision(aiGenerationTenantPolicy));

        filter.doFilter(request, response, filterChain);

        verify(rateLimiter).tryAcquire(
                new RateLimitKey(RateLimitScope.LLM_GENERATION_BY_TENANT, tenantId.toString()),
                aiGenerationTenantPolicy
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void appliesPatientReadsLimitByAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UUID.randomUUID());

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/patients/" + UUID.randomUUID()
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(rateLimiter.tryAcquire(
                eq(new RateLimitKey(RateLimitScope.PATIENT_READS_BY_USER, userId.toString())),
                eq(patientReadsPolicy)
        )).thenReturn(allowedDecision(patientReadsPolicy));

        filter.doFilter(request, response, filterChain);

        verify(rateLimiter).tryAcquire(
                new RateLimitKey(RateLimitScope.PATIENT_READS_BY_USER, userId.toString()),
                patientReadsPolicy
        );
        verify(filterChain).doFilter(request, response);
    }

    private RateLimitDecision allowedDecision(RateLimitPolicy policy) {
        return new RateLimitDecision(
                true,
                policy.permittedRequests(),
                policy.permittedRequests() - 1,
                Instant.now().plus(policy.window())
        );
    }

    private void authenticate(UUID userId, UUID tenantId) {
        CurrentUser currentUser = new CurrentUser(
                userId,
                tenantId,
                "doctor@example.com",
                "Dr. Test",
                UserRole.DOCTOR,
                UUID.randomUUID()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
        );
    }
}
