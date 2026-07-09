package com.pulse.posts.like;

import com.pulse.posts.like.dto.LikeResponse;
import com.pulse.posts.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/posts/{postId}/likes")
@Tag(name = "Likes", description = "Like and unlike posts; totals broadcast in real time over /topic/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    @Operation(summary = "Like a post (idempotent)",
            description = "Runs the sp_register_like stored procedure and broadcasts the new total "
                    + "to every WebSocket subscriber. Liking twice is a no-op.")
    public LikeResponse like(@PathVariable UUID postId,
                             @AuthenticationPrincipal AuthenticatedUser user) {
        return likeService.like(postId, user);
    }

    @DeleteMapping
    @Operation(summary = "Remove a like",
            description = "Runs the sp_remove_like stored procedure and broadcasts the new total.")
    public LikeResponse unlike(@PathVariable UUID postId,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        return likeService.unlike(postId, user);
    }
}
