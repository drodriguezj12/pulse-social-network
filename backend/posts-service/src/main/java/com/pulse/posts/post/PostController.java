package com.pulse.posts.post;

import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.PostResponse;
import com.pulse.posts.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/posts")
@Tag(name = "Posts", description = "Feed and post creation")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Operation(summary = "List other users' posts",
            description = "Feed of posts by users other than the token owner, newest first, "
                    + "with like totals and whether the current user liked each post.")
    public List<PostResponse> feed(@AuthenticationPrincipal AuthenticatedUser user) {
        return postService.getFeed(user);
    }

    @PostMapping
    @Operation(summary = "Create a post",
            description = "The author is taken from the JWT and the publication date is set on save.")
    public ResponseEntity<PostResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(user, request));
    }
}
