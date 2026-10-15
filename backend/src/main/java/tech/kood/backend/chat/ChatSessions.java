package tech.kood.backend.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Who has which sockets open. A user can have several (one per tab), keyed by session id.
@Component
public class ChatSessions {
    private static final Logger log = LoggerFactory.getLogger(ChatSessions.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_LIMIT_BYTES = 512 * 1024;

    private final Map<UUID, Map<String, WebSocketSession>> byUser = new ConcurrentHashMap<>();
    private final ObjectMapper json;

    ChatSessions(ObjectMapper json) {
        this.json = json;
    }

    public void add(UUID userId, WebSocketSession session) {
        // The decorator makes sendMessage safe when two events go to the same socket at once
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES);
        // Inside compute, so a concurrent remove can't drop the inner map while we add to it
        byUser.compute(userId, (id, sessions) -> {
            Map<String, WebSocketSession> map = sessions == null ? new ConcurrentHashMap<>() : sessions;
            map.put(session.getId(), safe);
            return map;
        });
    }

    public void remove(UUID userId, WebSocketSession session) {
        byUser.computeIfPresent(userId, (id, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void sendTo(UUID userId, Object event) {
        Map<String, WebSocketSession> sessions = byUser.get(userId);
        if (sessions == null) {
            return;
        }
        TextMessage message = new TextMessage(json.writeValueAsString(event));
        for (WebSocketSession session : sessions.values()) {
            try {
                session.sendMessage(message);
            } catch (IOException | RuntimeException e) {
                // A dead or overloaded socket (the decorator throws SessionLimitExceededException,
                // a RuntimeException): drop it; the browser reconnects and reloads what it missed
                log.debug("Dropping socket {} of user {}: {}", session.getId(), userId, e.getMessage());
                remove(userId, session);
            }
        }
    }
}
