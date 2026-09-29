package ro.scribemed.backend.identity.api;

import ro.scribemed.backend.identity.dto.AuthenticatedUserResponse;

public record LoginResponse(AuthenticatedUserResponse user) {
}
