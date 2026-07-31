package ro.scribemed.backend.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SessionTokenServiceTests {

    private final SessionTokenService sessionTokenService = new SessionTokenService();

    @Test
    void createsDifferentOpaqueTokens() {
        String firstToken = sessionTokenService.createRawToken();
        String secondToken = sessionTokenService.createRawToken();

        assertThat(firstToken).isNotBlank();
        assertThat(secondToken).isNotBlank();
        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    void hashesTokensDeterministicallyWithoutReturningTheRawToken() {
        String rawToken = "raw-session-token";

        String firstHash = sessionTokenService.hashToken(rawToken);
        String secondHash = sessionTokenService.hashToken(rawToken);

        assertThat(firstHash).isEqualTo(secondHash);
        assertThat(firstHash).isNotEqualTo(rawToken);
        assertThat(firstHash).hasSize(64);
    }
}
