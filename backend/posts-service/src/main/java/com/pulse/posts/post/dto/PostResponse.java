package com.pulse.posts.post.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        String authorAlias,
        String message,
        OffsetDateTime publishedAt,
        long likeCount,
        boolean likedByMe) {
}
