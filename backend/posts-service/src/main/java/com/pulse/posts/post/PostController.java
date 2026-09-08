package com.pulse.posts.post;

import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.FeedCursor;
import com.pulse.posts.post.dto.PostPage;
import com.pulse.posts.post.dto.PostResponse;
import com.pulse.posts.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@Validated
@Tag(name = "Posts", description = "Feed and post creation (text, optionally with an image)")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Operation(summary = "Read a page of the feed",
            description = "Posts from every user, newest first, with like totals and whether the "
                    + "current user liked each one. Pagination is cursor based: pass the "
                    + "nextCursor returned by the previous page. A null nextCursor means the end.")
    public PostPage feed(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Page size, 1 to 50")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @Parameter(description = "nextCursor from the previous page; omit for the first page")
            @RequestParam(required = false) String cursor) {
        return postService.getFeed(user, cursor == null ? null : FeedCursor.decode(cursor), limit);
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
            @RequestParam("message")
            @NotBlank(message = "message is required")
            @Size(max = 500, message = "message must be at most 500 characters") String message,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.create(user, new CreatePostRequest(message), image));
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
            description = "Only the author can delete a post. The removal is broadcast to "
                    + "/topic/posts-deleted so open feeds drop the card immediately.")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID postId) {
        postService.delete(user, postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/author-alias")
    @Operation(summary = "Sync the caller's denormalized alias onto their posts",
            description = "Called after renaming the profile in auth-service. Idempotent: the alias "
                    + "comes from the JWT, so replaying it changes nothing.")
    public ResponseEntity<Void> syncAuthorAlias(@AuthenticationPrincipal AuthenticatedUser user) {
        postService.syncAuthorAlias(user);
        return ResponseEntity.noContent().build();
    }
}
