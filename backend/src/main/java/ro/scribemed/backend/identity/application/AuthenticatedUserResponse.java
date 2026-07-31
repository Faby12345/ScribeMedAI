package ro.scribemed.backend.identity.application;

import java.util.UUID;

import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;

public record AuthenticatedUserResponse(
        UUID id,
        UUID tenantId,
        String email,
        String displayName,
        UserRole role
) {

    public static AuthenticatedUserResponse from(AppUser user) {
        return new AuthenticatedUserResponse(
                user.getId(),
                user.getTenant().getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole()
        );
    }
}
