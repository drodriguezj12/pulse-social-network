package com.pulse.posts.post;

import com.pulse.posts.common.InvalidImageException;
import com.pulse.posts.common.NotFoundException;
import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.PostResponse;
import com.pulse.posts.security.AuthenticatedUser;
import com.pulse.posts.ws.PostBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    /** Whitelist of image types a post attachment accepts. */
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostFeedDao postFeedDao;
    private final PostBroadcaster postBroadcaster;

    public PostService(PostRepository postRepository, PostImageRepository postImageRepository,
                       PostFeedDao postFeedDao, PostBroadcaster postBroadcaster) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.postFeedDao = postFeedDao;
        this.postBroadcaster = postBroadcaster;
    }

    @Transactional
    public List<PostResponse> getFeed(AuthenticatedUser user) {
        postRepository.updateAuthorAlias(user.id(), user.alias());
        return postFeedDao.findFeedFor(user.id());
    }

    /**
     * Creates a post with an optional image, then broadcasts it to /topic/posts
     * so every open feed shows it in real time.
     */
    @Transactional
    public PostResponse create(AuthenticatedUser author, CreatePostRequest request, MultipartFile image) {
        // saveAndFlush so @CreationTimestamp populates publishedAt before we map the response
        PostEntity post = postRepository.saveAndFlush(
                new PostEntity(author.id(), author.alias(), request.message().trim()));

        boolean hasImage = image != null && !image.isEmpty();
        if (hasImage) {
            postImageRepository.save(new PostImageEntity(
                    post.getId(), validatedContentType(image), imageBytes(image)));
        }

        log.info("AUDIT post_created postId={} authorId={} username={} hasImage={}",
                post.getId(), author.id(), author.username(), hasImage);

        PostResponse response = new PostResponse(post.getId(), post.getAuthorId(), post.getAuthorAlias(),
                post.getMessage(), post.getPublishedAt(), 0, false, hasImage);
        postBroadcaster.broadcast(response);
        return response;
    }

    @Transactional(readOnly = true)
    public PostImageEntity getImage(UUID postId) {
        return postImageRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post image not found"));
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        if (!post.getAuthorId().equals(user.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes eliminar tus propias publicaciones");
        }
        postRepository.delete(post);
        log.info("AUDIT post_deleted postId={} authorId={} username={}",
                postId, user.id(), user.username());
    }

    private String validatedContentType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidImageException("Only JPEG, PNG or WebP images are allowed");
        }
        return contentType;
    }

    private byte[] imageBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("Could not read the uploaded image");
        }
    }
}
