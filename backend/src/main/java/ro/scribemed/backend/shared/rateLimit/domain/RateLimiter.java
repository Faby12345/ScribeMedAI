package ro.scribemed.backend.shared.rateLimit.domain;

public interface RateLimiter {
    RateLimitDecision tryAcquire(
            RateLimitKey key,
            RateLimitPolicy policy
    );
}