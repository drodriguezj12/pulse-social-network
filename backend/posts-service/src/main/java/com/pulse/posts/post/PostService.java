package com.pulse.posts.post;

import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.PostResponse;
import com.pulse.posts.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final PostFeedDao postFeedDao;

    public PostService(PostRepository postRepository, PostFeedDao postFeedDao) {
        this.postRepository = postRepository;
        this.postFeedDao = postFeedDao;
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getFeed(AuthenticatedUser user) {
        return postFeedDao.findFeedFor(user.id());
    }

    @Transactional
    public PostResponse create(AuthenticatedUser author, CreatePostRequest request) {
        // saveAndFlush so @CreationTimestamp populates publishedAt before we map the response
        PostEntity post = postRepository.saveAndFlush(
                new PostEntity(author.id(), author.alias(), request.message().trim()));
        log.info("AUDIT post_created postId={} authorId={} username={}",
                post.getId(), author.id(), author.username());
        return new PostResponse(post.getId(), post.getAuthorId(), post.getAuthorAlias(),
                post.getMessage(), post.getPublishedAt(), 0, false);
    }
}
