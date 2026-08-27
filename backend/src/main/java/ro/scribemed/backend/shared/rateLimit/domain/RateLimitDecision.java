package ro.scribemed.backend.shared.rateLimit.domain;

import java.time.Instant;

public record RateLimitDecision(
        boolean allowed,
        int limit,
        int remaining,
        Instant resetAt
) {}