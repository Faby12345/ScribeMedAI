package ro.scribemed.backend.identity.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.scribemed.backend.identity.infrastructure.UserSessionRepository;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_COOKIE_NAME = "scribemed_session";

    private final UserSessionRepository userSessionRepository;
    private final SessionTokenService sessionTokenService;

    public SessionAuthenticationFilter(
            UserSessionRepository userSessionRepository,
            SessionTokenService sessionTokenService
    ) {
        this.userSessionRepository = userSessionRepository;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String rawSessionToken = readSessionCookie(request);

        if (rawSessionToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(rawSessionToken);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String rawSessionToken) {
        String sessionTokenHash = sessionTokenService.hashToken(rawSessionToken);
        userSessionRepository.findActiveBySessionTokenHashWithUserAndTenant(
                sessionTokenHash,
                Instant.now()
        ).ifPresent(session -> {
            CurrentUser currentUser = new CurrentUser(
                    session.getUser().getId(),
                    session.getTenant().getId(),
                    session.getUser().getEmail(),
                    session.getUser().getDisplayName(),
                    session.getUser().getRole(),
                    session.getId()
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + currentUser.role().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
    }

    private String readSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
