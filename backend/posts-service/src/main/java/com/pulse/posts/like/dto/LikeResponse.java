package com.pulse.posts.like.dto;

import java.util.UUID;

public record LikeResponse(
        UUID postId,
        long likeCount,
        boolean likedByMe) {
}
