package tech.kood.backend.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
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

    // True when this is the user's first open socket, i.e. they just came online
    public boolean add(UUID userId, WebSocketSession session) {
        // The decorator makes sendMessage safe when two events go to the same socket at once
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES);
        boolean[] first = {false};
        // Inside compute, so a concurrent remove can't drop the inner map while we add to it
        byUser.compute(userId, (id, sessions) -> {
            Map<String, WebSocketSession> map = sessions == null ? new ConcurrentHashMap<>() : sessions;
            first[0] = map.isEmpty();
            map.put(session.getId(), safe);
            return map;
        });
        return first[0];
    }

    // True when this was the user's last open socket, i.e. they just went offline
    public boolean remove(UUID userId, WebSocketSession session) {
        boolean[] last = {false};
        byUser.computeIfPresent(userId, (id, sessions) -> {
            boolean removed = sessions.remove(session.getId()) != null;
            if (sessions.isEmpty()) {
                last[0] = removed;
                return null;
            }
            return sessions;
        });
        return last[0];
    }

    public boolean isOnline(UUID userId) {
        return byUser.containsKey(userId);
    }

    public void sendTo(UUID userId, Object event) {
        Map<String, WebSocketSession> sessions = byUser.get(userId);
        if (sessions == null) {
            return;
        }
        TextMessage message = new TextMessage(json.writeValueAsString(event));
        sessions.values().forEach(session -> send(userId, session, message));
    }

    // One specific socket (tab), e.g. the one that asked for a user's status
    public void sendToSocket(UUID userId, String socketId, Object event) {
        Map<String, WebSocketSession> sessions = byUser.get(userId);
        WebSocketSession session = sessions == null ? null : sessions.get(socketId);
        if (session != null) {
            send(userId, session, new TextMessage(json.writeValueAsString(event)));
        }
    }

    private void send(UUID userId, WebSocketSession session, TextMessage message) {
        try {
            session.sendMessage(message);
        } catch (IOException | RuntimeException e) {
            // A dead or overloaded socket (the decorator throws SessionLimitExceededException, a
            // RuntimeException). Close it rather than just forgetting it: the close runs the normal
            // cleanup, including the "went offline" announcement. The browser reconnects and catches up.
            log.debug("Closing socket {} of user {}: {}", session.getId(), userId, e.getMessage());
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (IOException | RuntimeException closeFailed) {
                // Don't remove it here: that would skip the "went offline" check. The container's own
                // error handling still ends in afterConnectionClosed, which does it properly.
                log.debug("Closing socket {} failed: {}", session.getId(), closeFailed.getMessage());
            }
        }
    }
}
