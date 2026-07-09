package com.pulse.posts.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pushes like totals to every connected client so the feed updates in real
 * time without reloading (technical test requirement).
 */
@Component
public class LikeBroadcaster {

    public static final String LIKES_TOPIC = "/topic/likes";

    private static final Logger log = LoggerFactory.getLogger(LikeBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    public LikeBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(UUID postId, long likeCount) {
        messagingTemplate.convertAndSend(LIKES_TOPIC, new LikeEvent(postId, likeCount));
        log.debug("Broadcast like update postId={} likeCount={}", postId, likeCount);
    }
}
