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

    @Test
    @DisplayName("a like via REST is broadcast to WebSocket subscribers in real time")
    void likeIsBroadcastToSubscribers() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

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
