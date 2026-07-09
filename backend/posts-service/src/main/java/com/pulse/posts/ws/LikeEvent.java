package com.pulse.posts.ws;

import java.util.UUID;

/** Payload broadcast to /topic/likes whenever a like total changes. */
public record LikeEvent(UUID postId, long likeCount) {
}
