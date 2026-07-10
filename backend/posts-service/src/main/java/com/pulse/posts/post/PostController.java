package com.pulse.posts.post;

import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.PostResponse;
import com.pulse.posts.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@Tag(name = "Posts", description = "Feed and post creation (text, optionally with an image)")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Operation(summary = "List feed posts",
            description = "Feed of posts from every user, newest first, "
                    + "with like totals and whether the current user liked each post.")
    public List<PostResponse> feed(@AuthenticationPrincipal AuthenticatedUser user) {
        return postService.getFeed(user);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a text post",
            description = "The author is taken from the JWT and the publication date is set on save.")
    public ResponseEntity<PostResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.create(user, request, null));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a post with an optional image",
            description = "Multipart parts: 'message' (text) and optional 'image' (JPEG/PNG/WebP up to 2MB).")
    public ResponseEntity<PostResponse> createWithImage(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("message") String message,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        CreatePostRequest request = new CreatePostRequest(message);
        validate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.create(user, request, image));
    }

    /**
     * Public on purpose: post images render with plain img tags (which cannot
     * attach Authorization headers). Only image bytes are exposed.
     */
    @GetMapping("/{postId}/image")
    @Operation(summary = "Get a post's image (public)",
            description = "Returns the image bytes, or 404 when the post has no image.")
    public ResponseEntity<byte[]> image(@PathVariable UUID postId) {
        PostImageEntity image = postService.getImage(postId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)))  // immutable once created
                .body(image.getImage());
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete one of the authenticated user's posts",
            description = "Only the author of a post can delete it.")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID postId) {
        postService.delete(user, postId);
        return ResponseEntity.noContent().build();
    }

    /** Manual Bean Validation for the multipart variant (no @RequestBody there). */
    private void validate(CreatePostRequest request) {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);
            if (!violations.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        violations.iterator().next().getMessage());
            }
        }
    }
}
