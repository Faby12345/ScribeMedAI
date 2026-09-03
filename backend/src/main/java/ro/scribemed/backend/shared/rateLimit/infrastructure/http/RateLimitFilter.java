package ro.scribemed.backend.shared.rateLimit.infrastructure.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.shared.rateLimit.domain.*;

import java.io.IOException;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter rateLimiter;
    private final RateLimitPolicy loginPolicy;
    private final RateLimitPolicy uploadAudioPolicy;
    private final RateLimitPolicy aiGenerationTenantPolicy;
    private final RateLimitPolicy patientReadsPolicy;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            @Qualifier("loginRateLimitPolicy") RateLimitPolicy loginPolicy,
            @Qualifier("uploadAudioRateLimitPolicy") RateLimitPolicy uploadAudioPolicy,
            @Qualifier("aiGenerationTenantRateLimitPolicy") RateLimitPolicy aiGenerationTenantPolicy,
            @Qualifier("patientReadsRateLimitPolicy") RateLimitPolicy patientReadsPolicy
    ) {
        this.rateLimiter = rateLimiter;
        this.loginPolicy = loginPolicy;
        this.uploadAudioPolicy = uploadAudioPolicy;
        this.aiGenerationTenantPolicy = aiGenerationTenantPolicy;
        this.patientReadsPolicy = patientReadsPolicy;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException
    {

        RateLimitScope apiScope = determineApiScope(request);
        if(apiScope == null) {
            filterChain.doFilter(request, response);
            return;
        }


        boolean allowed = switch (apiScope) {
            case LOGIN_BY_IP -> handleLoginByIpScope(request, response);
            case TRANSCRIPTION_BY_USER -> true;
            case LLM_GENERATION_BY_TENANT -> handleAiGenerationScope(response);
            case PATIENT_READS_BY_USER -> handlePatientReadsScope(response);
            case AUDIO_UPLOAD_BY_USER -> handleAudioUploadScope(response);
            default -> true;
        };

        if(!allowed){
            return;
        }

        filterChain.doFilter(request, response);
    }



    private boolean handleLoginByIpScope(HttpServletRequest request, HttpServletResponse response) {
        return applyLimit(
                RateLimitScope.LOGIN_BY_IP,
                resolveClientIp(request),
                loginPolicy,
                response
        );
    }

    private CurrentUser getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof
                CurrentUser currentUser)) {
            return null;
        }

        return currentUser;
    }

    private boolean handleAudioUploadScope(HttpServletResponse response) {
        CurrentUser currentUser = getCurrentUser();
        if (currentUser == null) {
            // Do not rate-limit by user when no authenticated user exists.
            // Spring Security will reject the request later.
            return true;
        }

        return applyLimit(
                RateLimitScope.AUDIO_UPLOAD_BY_USER,
                currentUser.userId().toString(),
                uploadAudioPolicy,
                response
        );
    }

    private boolean handleAiGenerationScope(HttpServletResponse response) {
        CurrentUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return true;
        }

        return applyLimit(
                RateLimitScope.LLM_GENERATION_BY_TENANT,
                currentUser.tenantId().toString(),
                aiGenerationTenantPolicy,
                response
        );
    }

    private boolean handlePatientReadsScope(HttpServletResponse response) {
        CurrentUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return true;
        }

        return applyLimit(
                RateLimitScope.PATIENT_READS_BY_USER,
                currentUser.userId().toString(),
                patientReadsPolicy,
                response
        );
    }

    private boolean applyLimit(
            RateLimitScope scope,
            String identifier,
            RateLimitPolicy policy,
            HttpServletResponse response
    ) {
        RateLimitDecision decision = rateLimiter.tryAcquire(
                new RateLimitKey(scope, identifier),
                policy
        );
        addRateLimitHeaders(response, decision);

        if (decision.allowed()) {
            return true;
        }

        long retryAfterSeconds = Math.max(
                1,
                decision.resetAt().getEpochSecond() - Instant.now().getEpochSecond()
        );
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        return false;
    }

    private RateLimitScope determineApiScope(HttpServletRequest request) {

        if(isLoginRequest(request)) {
            return RateLimitScope.LOGIN_BY_IP;
        }
        if(isUploadAudioRequest(request)){
            return RateLimitScope.AUDIO_UPLOAD_BY_USER;
        }
        if (isAiGenerationRequest(request)) {
            return RateLimitScope.LLM_GENERATION_BY_TENANT;
        }
        if (isPatientReadRequest(request)) {
            return RateLimitScope.PATIENT_READS_BY_USER;
        }

        return null;
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return request.getMethod().equalsIgnoreCase("POST")
                && request.getRequestURI().equals("/api/v1/auth/login");
    }

    private boolean isUploadAudioRequest(HttpServletRequest request) {
        return request.getMethod().equalsIgnoreCase("POST")
                && request.getRequestURI().matches(
                "^/api/v1/consultations/[^/]+/audio$"
        );
    }

    private boolean isAiGenerationRequest(HttpServletRequest request) {
        return request.getMethod().equalsIgnoreCase("POST")
                && request.getRequestURI().matches(
                "^/api/v1/consultations/[^/]+/notes$"
        );
    }

    private boolean isPatientReadRequest(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return false;
        }

        String requestUri = request.getRequestURI();
        return requestUri.equals("/api/v1/patients")
                || requestUri.matches("^/api/v1/patients/[^/]+$");
    }

    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitDecision decision
    ) {
        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(decision.limit())
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(decision.remaining())
        );

        response.setHeader(
                "X-RateLimit-Reset",
                String.valueOf(decision.resetAt().getEpochSecond())
        );
    }
}
