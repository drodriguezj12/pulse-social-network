package com.pulse.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    @Test
    @DisplayName("issued token round-trips subject, username and alias")
    void tokenRoundTrip() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "mariana", "marilo");
        AuthenticatedUser parsed = jwtService.parse(token);

        assertThat(parsed.id()).isEqualTo(userId);
        assertThat(parsed.username()).isEqualTo("mariana");
        assertThat(parsed.alias()).isEqualTo("marilo");
    }

    @Test
    @DisplayName("expired token is rejected")
    void expiredTokenRejected() {
        JwtService shortLived = new JwtService(SECRET, -1_000L);
        String token = shortLived.generateToken(UUID.randomUUID(), "carlos", "cgomez");

        assertThatThrownBy(() -> shortLived.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("token signed with a different secret is rejected")
    void tamperedTokenRejected() {
        JwtService other = new JwtService("another-secret-0123456789-0123456789-01234", 3_600_000L);
        String token = other.generateToken(UUID.randomUUID(), "valentina", "valen");

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("garbage input is rejected")
    void garbageRejected() {
        assertThatThrownBy(() -> jwtService.parse("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
