package com.pulse.posts.post.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedCursorTest {

    @Test
    @DisplayName("a cursor survives the round trip")
    void roundTrip() {
        FeedCursor original = new FeedCursor(OffsetDateTime.parse("2026-05-04T10:15:30Z"), UUID.randomUUID());

        FeedCursor decoded = FeedCursor.decode(original.encode());

        assertThat(decoded.id()).isEqualTo(original.id());
        assertThat(decoded.publishedAt().toInstant()).isEqualTo(original.publishedAt().toInstant());
    }

    @Test
    @DisplayName("the token is URL safe, so it needs no escaping as a query parameter")
    void tokenIsUrlSafe() {
        String token = new FeedCursor(OffsetDateTime.now(), UUID.randomUUID()).encode();

        assertThat(token).doesNotContain("+", "/", "=");
    }

    @Test
    @DisplayName("tokens this API did not issue are rejected as bad requests")
    void invalidTokensRejected() {
        for (String bad : new String[]{"not-a-cursor", "", "%%%", "dGhpcyBpcyBub3QgYSBjdXJzb3I"}) {
            assertThatThrownBy(() -> FeedCursor.decode(bad))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid cursor");
        }
    }
}
