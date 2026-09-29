package ro.scribemed.backend.identity.security;

import java.util.UUID;

import ro.scribemed.backend.identity.domain.UserRole;

public record CurrentUser(
        UUID userId,
        UUID tenantId,
        String email,
        String displayName,
        UserRole role,
        UUID sessionId
) {
}
