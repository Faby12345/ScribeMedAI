package ro.scribemed.backend.identity.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserSession;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.identity.dto.AuthenticatedUserResponse;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.identity.infrastructure.UserSessionRepository;
import ro.scribemed.backend.identity.security.SessionTokenService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration SESSION_DURATION = Duration.ofHours(12);

    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;
    private final AuditService auditService;

    public AuthService(
            AppUserRepository appUserRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            SessionTokenService sessionTokenService,
            AuditService auditService
    ) {
        this.appUserRepository = appUserRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResult login(String email, String password) {
        AppUser user = appUserRepository.findByEmailAndStatus(email.strip(), UserStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.info("auth_login_failed status=bad_credentials");
            throw new AuthenticationException();
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_DURATION);
        String rawSessionToken = sessionTokenService.createRawToken();
        String sessionTokenHash = sessionTokenService.hashToken(rawSessionToken);

        UserSession session = userSessionRepository.save(new UserSession(
                user.getTenant(),
                user,
                sessionTokenHash,
                expiresAt
        ));
        user.markLoggedIn(now);

        auditService.record(
                user.getTenant(),
                user,
                "AUTH_LOGIN_SUCCEEDED",
                "user_session",
                session.getId(),
                Map.of("status", "success")
        );
        log.info(
                "auth_login_succeeded tenantId={} userId={} sessionId={}",
                user.getTenant().getId(),
                user.getId(),
                session.getId()
        );

        return new LoginResult(rawSessionToken, expiresAt, AuthenticatedUserResponse.from(user));
    }

    @Transactional
    public void logout(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return;
        }

        String sessionTokenHash = sessionTokenService.hashToken(rawSessionToken);
        userSessionRepository.findBySessionTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                sessionTokenHash,
                Instant.now()
        ).ifPresent(session -> {
            session.revoke(Instant.now());
            auditService.record(
                    session.getTenant(),
                    session.getUser(),
                    "AUTH_LOGOUT",
                    "user_session",
                    session.getId(),
                    Map.of("status", "success")
            );
            log.info(
                    "auth_logout tenantId={} userId={} sessionId={}",
                    session.getTenant().getId(),
                    session.getUser().getId(),
                    session.getId()
            );
        });
    }
}
