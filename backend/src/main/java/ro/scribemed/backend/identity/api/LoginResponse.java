package ro.scribemed.backend.identity.api;

import ro.scribemed.backend.identity.application.AuthenticatedUserResponse;

public record LoginResponse(AuthenticatedUserResponse user) {
}
