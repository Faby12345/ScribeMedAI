package ro.scribemed.backend.shared.rateLimit.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.shared.rateLimit.domain.*;
import ro.scribemed.backend.shared.rateLimit.infrastructure.persistence.RateLimitWindowRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@Service
public class FixedWindowRateLimiter implements RateLimiter {


    private final RateLimitWindowRepository rateLimitWindowRepository;
    private final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

    public FixedWindowRateLimiter(RateLimitWindowRepository rateLimitWindowRepository) {
        this.rateLimitWindowRepository = rateLimitWindowRepository;
    }



    @Transactional
    @Override
    public RateLimitDecision tryAcquire(
            RateLimitKey key,
            RateLimitPolicy policy
    ) {
        Instant now = Instant.now();

        RateLimitWindow currentWindow = getOrCreateWindow(key, now);

        boolean expired = Duration
                .between(currentWindow.getWindowStartedAt(), now)
                .compareTo(policy.window()) >= 0;

        log.info(
                "SCOPE: {}, IDENTIFIER: {}",
                key.scope(),
                key.identifier()
        );

        if (expired) {
            currentWindow.reset(now);     // count = 0
            currentWindow.increment(now); // count = 1

            Instant resetAt = currentWindow
                    .getWindowStartedAt()
                    .plus(policy.window());

            return new RateLimitDecision(
                    true,
                    policy.permittedRequests(),
                    policy.permittedRequests() - currentWindow.getRequestCount(),
                    resetAt
            );
        }

        Instant resetAt = currentWindow
                .getWindowStartedAt()
                .plus(policy.window());

        if (currentWindow.getRequestCount() < policy.permittedRequests()) {
            currentWindow.increment(now);

            return new RateLimitDecision(
                    true,
                    policy.permittedRequests(),
                    policy.permittedRequests() - currentWindow.getRequestCount(),
                    resetAt
            );
        }

        return new RateLimitDecision(
                false,
                policy.permittedRequests(),
                0,
                resetAt
        );
    }

    private RateLimitWindow getOrCreateWindow(
            RateLimitKey key,
            Instant now
    ) {
        rateLimitWindowRepository.insertIfAbsent(
                UUID.randomUUID(),
                key.scope().name(),
                key.identifier(),
                now
        );

        return rateLimitWindowRepository
                .findByScopeAndIdentifier(
                        key.scope(),
                        key.identifier()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Rate-limit window could not be created"
                        )
                );
    }
}
