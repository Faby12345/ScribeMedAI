package ro.scribemed.backend.identity.api;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.scribemed.backend.identity.application.AuthService;
import ro.scribemed.backend.identity.application.AuthenticatedUserResponse;
import ro.scribemed.backend.identity.application.AuthenticationException;
import ro.scribemed.backend.identity.application.LoginResult;
import ro.scribemed.backend.identity.config.SessionCookieProperties;
import ro.scribemed.backend.identity.security.CurrentUser;
import ro.scribemed.backend.identity.security.SessionAuthenticationFilter;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionCookieProperties sessionCookieProperties;

    public AuthController(AuthService authService, SessionCookieProperties sessionCookieProperties) {
        this.authService = authService;
        this.sessionCookieProperties = sessionCookieProperties;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request.email(), request.password());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createSessionCookie(result).toString())
                .body(new LoginResponse(result.user()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(readSessionCookie(request));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearSessionCookie().toString())
                .build();
    }

    @GetMapping("/me")
    AuthenticatedUserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return new AuthenticatedUserResponse(
                currentUser.userId(),
                currentUser.tenantId(),
                currentUser.email(),
                currentUser.displayName(),
                currentUser.role()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Void> handleAuthenticationException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private ResponseCookie createSessionCookie(LoginResult result) {
        long maxAgeSeconds = Math.max(0, Duration.between(java.time.Instant.now(), result.expiresAt()).toSeconds());
        return ResponseCookie.from(SessionAuthenticationFilter.SESSION_COOKIE_NAME, result.rawSessionToken())
                .httpOnly(true)
                .secure(sessionCookieProperties.isSecure())
                .sameSite(sessionCookieProperties.getSameSite())
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie clearSessionCookie() {
        return ResponseCookie.from(SessionAuthenticationFilter.SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(sessionCookieProperties.isSecure())
                .sameSite(sessionCookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }

    private String readSessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (SessionAuthenticationFilter.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
