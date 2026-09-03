package ro.scribemed.backend.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import ro.scribemed.backend.identity.application.AuthService;
import ro.scribemed.backend.identity.application.LoginResult;
import ro.scribemed.backend.identity.config.SessionCookieProperties;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.dto.AuthenticatedUserResponse;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.identity.security.SessionAuthenticationFilter;

class AuthControllerTests {

    private final AuthService authService = mock(AuthService.class);
    private final SessionCookieProperties sessionCookieProperties = new SessionCookieProperties();
    private final AuthController controller = new AuthController(authService, sessionCookieProperties);

    @Test
    void returnsOkAndSetsSessionCookieOnLogin() {
        AuthenticatedUserResponse user = new AuthenticatedUserResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "doctor@example.com",
                "Dr. Test",
                UserRole.DOCTOR
        );
        when(authService.login("doctor@example.com", "correct-password"))
                .thenReturn(new LoginResult("opaque-session-token", Instant.now().plusSeconds(3600), user));

        ResponseEntity<LoginResponse> response = controller
                .login(new LoginRequest("doctor@example.com", "correct-password"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new LoginResponse(user));
        assertThat(response.getHeaders().getFirst("Set-Cookie"))
                .contains(SessionAuthenticationFilter.SESSION_COOKIE_NAME)
                .contains("HttpOnly");
    }

    @Test
    void returnsNoContentAndClearsSessionCookieOnLogout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SessionAuthenticationFilter.SESSION_COOKIE_NAME, "opaque-session-token"));

        ResponseEntity<Void> response = controller.logout(request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getHeaders().getFirst("Set-Cookie")).contains("Max-Age=0");
        verify(authService).logout("opaque-session-token");
    }

    @Test
    void returnsOkForCurrentAuthenticatedUser() {
        CurrentUser currentUser = currentUser();

        ResponseEntity<AuthenticatedUserResponse> response = controller.me(currentUser);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().id()).isEqualTo(currentUser.userId());
        assertThat(response.getBody().tenantId()).isEqualTo(currentUser.tenantId());
    }

    private CurrentUser currentUser() {
        return new CurrentUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "doctor@example.com",
                "Dr. Test",
                UserRole.DOCTOR,
                UUID.randomUUID()
        );
    }
}
