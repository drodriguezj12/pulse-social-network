package com.pulse.posts.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * One page of the feed. {@code nextCursor} is null when the last page has been
 * reached, so clients can simply stop asking.
 */
public record PostPage(
        List<PostResponse> items,
        @Schema(description = "Opaque token to pass as ?cursor= for the next page; null when there are no more posts",
                nullable = true)
        String nextCursor) {
}
