package ro.scribemed.backend.shared.rateLimit.domain;

public record RateLimitKey(
        RateLimitScope scope,
        String identifier
) {}