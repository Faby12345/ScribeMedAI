package ro.scribemed.backend.shared.rateLimit.infrastructure.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.scribemed.backend.shared.rateLimit.domain.*;

import java.io.IOException;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter rateLimiter;
    private final RateLimitPolicy loginPolicy;

    public RateLimitFilter(RateLimiter rateLimiter,
                           RateLimitPolicy loginPolicy) {
        this.rateLimiter = rateLimiter;
        this.loginPolicy = loginPolicy;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException
    {
        if(!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ipClient = resolveClientIp(request);

        RateLimitDecision decision = rateLimiter.tryAcquire(
                new RateLimitKey(RateLimitScope.LOGIN_BY_IP, ipClient),
                loginPolicy
        );

        addRateLimitHeaders(response, decision);

        if(!decision.allowed()){
            long retryAfterSeconds = Math.max(
                    1,
                    decision.resetAt().getEpochSecond() - Instant.now().getEpochSecond()
            );

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            return;
        }

        filterChain.doFilter(request, response);



    }
    private boolean isLoginRequest(HttpServletRequest request) {
        return request.getMethod().equalsIgnoreCase("POST")
                && request.getRequestURI().equals("/api/v1/auth/login");
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
