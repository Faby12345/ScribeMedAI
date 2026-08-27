package ro.scribemed.backend.shared.rateLimit.infrastructure;

import ro.scribemed.backend.shared.rateLimit.domain.RateLimitDecision;
import ro.scribemed.backend.shared.rateLimit.domain.RateLimitKey;
import ro.scribemed.backend.shared.rateLimit.domain.RateLimitPolicy;
import ro.scribemed.backend.shared.rateLimit.domain.RateLimiter;

public final class FixedWindowRateLimiter implements RateLimiter {
    @Override
    public RateLimitDecision tryAcquire(RateLimitKey key, RateLimitPolicy policy) {
        return null;
    }
}
