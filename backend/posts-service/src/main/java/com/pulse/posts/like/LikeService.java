package com.pulse.posts.like;

import com.pulse.posts.common.NotFoundException;
import com.pulse.posts.like.dto.LikeResponse;
import com.pulse.posts.post.PostRepository;
import com.pulse.posts.security.AuthenticatedUser;
import com.pulse.posts.ws.LikeBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeService.class);

    private final PostRepository postRepository;
    private final LikeDao likeDao;
    private final LikeBroadcaster likeBroadcaster;

    public LikeService(PostRepository postRepository, LikeDao likeDao, LikeBroadcaster likeBroadcaster) {
        this.postRepository = postRepository;
        this.likeDao = likeDao;
        this.likeBroadcaster = likeBroadcaster;
    }

    @Transactional
    public LikeResponse like(UUID postId, AuthenticatedUser user) {
        requirePostExists(postId);
        long total = likeDao.registerLike(postId, user.id());
        log.info("AUDIT like_registered postId={} userId={} username={} total={}",
                postId, user.id(), user.username(), total);
        likeBroadcaster.broadcast(postId, total);
        return new LikeResponse(postId, total, true);
    }

    @Transactional
    public LikeResponse unlike(UUID postId, AuthenticatedUser user) {
        requirePostExists(postId);
        long total = likeDao.removeLike(postId, user.id());
        log.info("AUDIT like_removed postId={} userId={} username={} total={}",
                postId, user.id(), user.username(), total);
        likeBroadcaster.broadcast(postId, total);
        return new LikeResponse(postId, total, false);
    }

    private void requirePostExists(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("Post not found");
        }
    }
}
