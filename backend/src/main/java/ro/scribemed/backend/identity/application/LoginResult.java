package ro.scribemed.backend.identity.application;

import java.time.Instant;

import ro.scribemed.backend.identity.dto.AuthenticatedUserResponse;

public record LoginResult(
        String rawSessionToken,
        Instant expiresAt,
        AuthenticatedUserResponse user
) {
}
