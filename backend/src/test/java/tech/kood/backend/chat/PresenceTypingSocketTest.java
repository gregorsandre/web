package tech.kood.backend.chat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tech.kood.backend.auth.JwtService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// Real sockets, so no test transaction: users are created per test and deleted afterwards
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.validate-on-migrate=false")
class PresenceTypingSocketTest {

    @LocalServerPort int port;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired ChatRepository chats;

    private final StandardWebSocketClient client = new StandardWebSocketClient();
    private final List<UUID> users = new ArrayList<>();
    private final List<WebSocketSession> sockets = new ArrayList<>();
    UUID alice;
    UUID bob;
    UUID carol;

    record Client(WebSocketSession socket, BlockingQueue<String> inbox) {
        void send(String json) throws Exception {
            socket.sendMessage(new TextMessage(json));
        }
    }

    @BeforeEach
    void setUp() {
        alice = user();
        bob = user();
        carol = user();
        jdbc.update("""
                INSERT INTO connections (requester_id, addressee_id, status, responded_at)
                VALUES (?, ?, 'accepted', now())""", alice, bob);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (WebSocketSession socket : sockets) {
            if (socket.isOpen()) socket.close();
        }
        for (UUID id : users) {
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    @Test
    void allowedWatchGetsStatusAndLiveChanges() throws Exception {
        Client a = connect(alice);
        a.send("{\"type\":\"watch\",\"userId\":\"" + bob + "\"}");
        assertNotNull(next(a, "\"type\":\"presence\",\"userId\":\"" + bob + "\",\"online\":false"));

        Client b = connect(bob);
        assertNotNull(next(a, "\"type\":\"presence\",\"userId\":\"" + bob + "\",\"online\":true"));

        b.socket().close();
        assertNotNull(next(a, "\"type\":\"presence\",\"userId\":\"" + bob + "\",\"online\":false"));
    }

    @Test
    void secondTabDoesNotFlipStatus() throws Exception {
        Client a = connect(alice);
        connect(bob);
        a.send("{\"type\":\"watch\",\"userId\":\"" + bob + "\"}");
        assertNotNull(next(a, "\"online\":true"));

        Client bobTab2 = connect(bob);
        bobTab2.socket().close();
        assertNull(next(a, "\"online\":false"));
    }

    @Test
    void pendingRequestAllowsWatching() throws Exception {
        // alice asked carol to connect: carol may view alice, as with the profile endpoints
        jdbc.update("INSERT INTO connections (requester_id, addressee_id, status) VALUES (?, ?, 'pending')", alice, carol);
        Client c = connect(carol);
        connect(alice);
        c.send("{\"type\":\"watch\",\"userId\":\"" + alice + "\"}");
        assertNotNull(next(c, "\"type\":\"presence\",\"userId\":\"" + alice + "\",\"online\":true"));
    }

    @Test
    void disallowedWatchGetsNothing() throws Exception {
        // carol has no connection, request or bio, so she may not view alice
        Client c = connect(carol);
        connect(alice);
        c.send("{\"type\":\"watch\",\"userId\":\"" + alice + "\"}");
        assertNull(next(c, "presence"));
    }

    @Test
    void typingReachesOnlyTheOtherParticipant() throws Exception {
        long chatId = chats.findOrCreateConversation(alice, bob);
        Client a = connect(alice);
        Client b = connect(bob);
        Client c = connect(carol);

        a.send("{\"type\":\"typing\",\"chatId\":" + chatId + "}");
        assertNotNull(next(b, "\"type\":\"typing\",\"chatId\":" + chatId + ",\"userId\":\"" + alice + "\""));
        assertNull(next(a, "typing"));
        assertNull(next(c, "typing"));

        // carol isn't in that chat: dropped
        c.send("{\"type\":\"typing\",\"chatId\":" + chatId + "}");
        assertNull(next(b, "typing"));
    }

    @Test
    void malformedEventsAreIgnored() throws Exception {
        Client a = connect(alice);
        a.send("not json");
        a.send("{\"type\":\"watch\",\"userId\":\"not-a-uuid\"}");
        a.send("{\"type\":\"typing\",\"chatId\":\"abc\"}");
        // the socket still works afterwards
        a.send("{\"type\":\"watch\",\"userId\":\"" + bob + "\"}");
        assertNotNull(next(a, "\"type\":\"presence\""));
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, 'x')", id, id + "@test.local");
        users.add(id);
        return id;
    }

    private Client connect(UUID userId) throws Exception {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        WebSocketSession socket = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                inbox.add(message.getPayload());
            }
        }, "ws://localhost:" + port + "/ws?token=" + jwt.create(userId)).get(5, TimeUnit.SECONDS);
        sockets.add(socket);
        return new Client(socket, inbox);
    }

    // The next message containing `fragment` within 1.5 s, or null; other messages are skipped
    private String next(Client client, String fragment) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1500;
        while (System.currentTimeMillis() < deadline) {
            String message = client.inbox().poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (message != null && message.contains(fragment)) return message;
        }
        return null;
    }
}
