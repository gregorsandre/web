package tech.kood.backend.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tech.kood.backend.auth.JwtService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.validate-on-migrate=false")
class ChatSocketTest {

    @LocalServerPort int port;
    @Autowired JwtService jwt;
    @Autowired ChatSessions sessions;

    private final StandardWebSocketClient client = new StandardWebSocketClient();

    @Test
    void refusesMissingToken() {
        assertThrows(ExecutionException.class,
                () -> client.execute(new TextWebSocketHandler(), url("")).get(5, TimeUnit.SECONDS));
    }

    @Test
    void refusesInvalidToken() {
        assertThrows(ExecutionException.class,
                () -> client.execute(new TextWebSocketHandler(), url("?token=nonsense")).get(5, TimeUnit.SECONDS));
    }

    @Test
    void deliversEventsToTheUsersSocket() throws Exception {
        UUID user = UUID.randomUUID();
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        WebSocketSession socket = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                received.add(message.getPayload());
            }
        }, url("?token=" + jwt.create(user))).get(5, TimeUnit.SECONDS);

        // The server registers the socket right after the handshake; retry briefly until it has
        String payload = null;
        for (int i = 0; i < 20 && payload == null; i++) {
            sessions.sendTo(user, Map.of("type", "ping"));
            payload = received.poll(100, TimeUnit.MILLISECONDS);
        }
        assertEquals("{\"type\":\"ping\"}", payload);

        // Events for someone else never reach this socket (extra pings from the retry loop may)
        sessions.sendTo(UUID.randomUUID(), Map.of("type", "not-for-you"));
        String later;
        while ((later = received.poll(300, TimeUnit.MILLISECONDS)) != null) {
            assertFalse(later.contains("not-for-you"));
        }
        socket.close();
    }

    private String url(String query) {
        return "ws://localhost:" + port + "/ws" + query;
    }
}
