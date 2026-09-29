package ro.scribemed.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserSession;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.identity.infrastructure.UserSessionRepository;
import ro.scribemed.backend.identity.security.SessionTokenService;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;

class AuthServiceTests {

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final UserSessionRepository userSessionRepository = mock(UserSessionRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SessionTokenService sessionTokenService = new SessionTokenService();
    private final AuditService auditService = mock(AuditService.class);

    private final AuthService authService = new AuthService(
            appUserRepository,
            userSessionRepository,
            passwordEncoder,
            sessionTokenService,
            auditService
    );

    @Test
    void loginCreatesSessionForActiveUserWithValidPassword() {
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser user = new AppUser(
                tenant,
                "doctor@example.com",
                passwordEncoder.encode("correct-password"),
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        when(appUserRepository.findByEmailAndStatus("doctor@example.com", UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(userSessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResult result = authService.login("doctor@example.com", "correct-password");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession savedSession = sessionCaptor.getValue();

        assertThat(result.rawSessionToken()).isNotBlank();
        assertThat(result.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(result.user().email()).isEqualTo("doctor@example.com");
        assertThat(result.user().displayName()).isEqualTo("Dr. Demo");
        assertThat(result.user().role()).isEqualTo(UserRole.DOCTOR);
        assertThat(savedSession.getSessionTokenHash()).isNotEqualTo(result.rawSessionToken());
        assertThat(savedSession.getExpiresAt()).isEqualTo(result.expiresAt());
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(auditService).record(
                eq(tenant),
                eq(user),
                eq("AUTH_LOGIN_SUCCEEDED"),
                eq("user_session"),
                any(),
                any()
        );
    }

    @Test
    void loginRejectsBadPassword() {
        Tenant tenant = new Tenant("Demo Clinic", TenantStatus.ACTIVE);
        AppUser user = new AppUser(
                tenant,
                "doctor@example.com",
                passwordEncoder.encode("correct-password"),
                "Dr. Demo",
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        when(appUserRepository.findByEmailAndStatus("doctor@example.com", UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("doctor@example.com", "wrong-password"))
                .isInstanceOf(AuthenticationException.class);

        verify(userSessionRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void loginRejectsUnknownUser() {
        when(appUserRepository.findByEmailAndStatus("doctor@example.com", UserStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("doctor@example.com", "password"))
                .isInstanceOf(AuthenticationException.class);

        verify(userSessionRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }
}
