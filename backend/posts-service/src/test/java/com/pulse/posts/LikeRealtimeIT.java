package com.pulse.posts;

import com.pulse.posts.security.JwtService;
import com.pulse.posts.ws.LikeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the real-time requirement end to end: a STOMP client subscribed to
 * /topic/likes receives the new total when another user likes a post via REST.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeRealtimeIT {

    private static final UUID DANIELA_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ANDRES_POST = UUID.fromString("10000000-0000-0000-0000-000000000004");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AbstractIntegrationTest::jdbcUrlWithCallMode);
        registry.add("spring.datasource.username", AbstractIntegrationTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AbstractIntegrationTest.POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private WebSocketStompClient stompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        // The context mapper knows java.time (OffsetDateTime in PostResponse)
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);
        return client;
    }

    @Test
    @DisplayName("a new post via REST is broadcast to /topic/posts in real time")
    void newPostIsBroadcastToSubscribers() throws Exception {
        StompSession session = stompClient()
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        CompletableFuture<com.pulse.posts.post.dto.PostResponse> received = new CompletableFuture<>();
        session.subscribe("/topic/posts", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return com.pulse.posts.post.dto.PostResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((com.pulse.posts.post.dto.PostResponse) payload);
            }
        });
        Thread.sleep(500);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken(DANIELA_ID, "daniela", "danim"));
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/posts", HttpMethod.POST,
                new HttpEntity<>("{\"message\":\"realtime post!\"}", headers), String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        var event = received.get(10, TimeUnit.SECONDS);
        assertThat(event.message()).isEqualTo("realtime post!");
        assertThat(event.authorAlias()).isEqualTo("danim");

        session.disconnect();
    }

    @Test
    @DisplayName("a like via REST is broadcast to WebSocket subscribers in real time")
    void likeIsBroadcastToSubscribers() throws Exception {
        WebSocketStompClient stompClient = stompClient();

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        CompletableFuture<LikeEvent> received = new CompletableFuture<>();
        session.subscribe("/topic/likes", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LikeEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((LikeEvent) payload);
            }
        });
        // Give the SUBSCRIBE frame a moment to be processed by the broker.
        Thread.sleep(500);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken(DANIELA_ID, "daniela", "danim"));
        ResponseEntity<String> response = restTemplate.exchange(
                "/posts/" + ANDRES_POST + "/likes", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        LikeEvent event = received.get(10, TimeUnit.SECONDS);
        assertThat(event.postId()).isEqualTo(ANDRES_POST);
        assertThat(event.likeCount()).isGreaterThanOrEqualTo(1);

        session.disconnect();
    }
}
