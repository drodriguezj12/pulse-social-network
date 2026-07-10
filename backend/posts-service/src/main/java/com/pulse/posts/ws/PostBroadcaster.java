package com.pulse.posts.ws;

import com.pulse.posts.post.dto.PostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes newly created posts to every connected client so open feeds grow in
 * real time. Clients ignore their own posts (the feed only shows other
 * users' publications).
 */
@Component
public class PostBroadcaster {

    public static final String POSTS_TOPIC = "/topic/posts";

    private static final Logger log = LoggerFactory.getLogger(PostBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    public PostBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(PostResponse post) {
        messagingTemplate.convertAndSend(POSTS_TOPIC, post);
        log.debug("Broadcast new post postId={} author={}", post.id(), post.authorAlias());
    }
}
