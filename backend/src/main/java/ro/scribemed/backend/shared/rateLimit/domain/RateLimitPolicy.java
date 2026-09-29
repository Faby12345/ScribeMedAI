package ro.scribemed.backend.shared.rateLimit.domain;

import java.time.Duration;

public record RateLimitPolicy(
        int permittedRequests,
        Duration window
) {
}
