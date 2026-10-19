package tech.kood.backend.chat;

import jakarta.websocket.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

// Keeps ChatSessions in step with open sockets, announces online/offline, and handles the
// events browsers send: watch / unwatch a user's online status, and typing in a chat.
@Component
public class ChatSocketHandler extends TextWebSocketHandler {
    private final PresenceWatchers watchers;
    private final ChatService chat;
    private final ObjectMapper json;

    ChatSocketHandler(PresenceWatchers watchers, ChatService chat, ObjectMapper json) {
        this.watchers = watchers;
        this.chat = chat;
        this.json = json;
    }

    // Browsers ping every 25 s; a socket silent for longer than this is a lost connection (Wi-Fi off,
    // laptop asleep) and gets closed, so the user turns "Offline" instead of staying "Online" for hours
    static final long IDLE_TIMEOUT_MS = 60_000;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (session instanceof NativeWebSocketSession nativeSocket) {
            Session tomcatSession = nativeSocket.getNativeSession(Session.class);
            if (tomcatSession != null) {
                tomcatSession.setMaxIdleTimeout(IDLE_TIMEOUT_MS);
            }
        }
        watchers.connected(userId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        watchers.disconnected(userId(session), session);
    }

    // Anything malformed or unknown is ignored; the browser doesn't wait for answers
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonNode event;
        try {
            event = json.readTree(message.getPayload());
        } catch (JacksonException e) {
            return;
        }
        UUID me = userId(session);
        switch (event.path("type").asString("")) {
            case "watch" -> uuid(event.path("userId")).ifPresent(id -> watchers.watch(me, session.getId(), id));
            case "unwatch" -> uuid(event.path("userId")).ifPresent(id -> watchers.unwatch(session.getId(), id));
            // "ping" (the browser's keep-alive) needs no answer: receiving it resets the idle timer
            case "typing" -> {
                JsonNode chatId = event.path("chatId");
                if (chatId.isIntegralNumber() && chatId.canConvertToLong()) {
                    chat.typing(me, chatId.asLong());
                }
            }
            default -> {
            }
        }
    }

    private static Optional<UUID> uuid(JsonNode node) {
        try {
            return Optional.of(UUID.fromString(node.asString("")));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static UUID userId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(TokenHandshakeInterceptor.USER_ID);
    }
}
