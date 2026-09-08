package com.pulse.posts.ws;

import com.pulse.posts.post.dto.PostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Pushes feed changes to every connected client, so open feeds grow and shrink
 * on their own instead of waiting for a reload.
 */
@Component
public class PostBroadcaster {

    public static final String CREATED_TOPIC = "/topic/posts";
    public static final String DELETED_TOPIC = "/topic/posts-deleted";

    private static final Logger log = LoggerFactory.getLogger(PostBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    public PostBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastCreated(PostResponse post) {
        messagingTemplate.convertAndSend(CREATED_TOPIC, post);
        log.debug("Broadcast new post postId={} author={}", post.id(), post.authorAlias());
    }

    public void broadcastDeleted(UUID postId) {
        messagingTemplate.convertAndSend(DELETED_TOPIC, Map.of("postId", postId.toString()));
        log.debug("Broadcast deleted post postId={}", postId);
    }
}
