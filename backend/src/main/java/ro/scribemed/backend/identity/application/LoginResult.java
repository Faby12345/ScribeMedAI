package ro.scribemed.backend.identity.application;

import java.time.Instant;

public record LoginResult(
        String rawSessionToken,
        Instant expiresAt,
        AuthenticatedUserResponse user
) {
}
