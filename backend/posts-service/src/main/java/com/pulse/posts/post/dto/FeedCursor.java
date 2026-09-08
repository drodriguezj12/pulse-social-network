package com.pulse.posts.post.dto;

import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Position of the last item a client received, as the (publishedAt, id) pair the
 * feed is sorted by.
 *
 * <p>It travels as one opaque Base64 token instead of two query parameters: the
 * client only has to echo back what the previous page returned, and the sort key
 * can change later without breaking the API contract.
 */
public record FeedCursor(OffsetDateTime publishedAt, UUID id) {

    private static final String SEPARATOR = "|";

    public String encode() {
        String raw = publishedAt + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** @throws ResponseStatusException 400 when the token is not a cursor this API issued */
    public static FeedCursor decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\" + SEPARATOR, 2);
            return new FeedCursor(OffsetDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid cursor");
        }
    }

    public static FeedCursor of(PostResponse post) {
        return new FeedCursor(post.publishedAt(), post.id());
    }
}
