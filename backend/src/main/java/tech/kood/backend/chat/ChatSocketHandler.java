package tech.kood.backend.chat;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

// Keeps ChatSessions in step with open sockets. Messages from the browser are not used yet
// (the typing indicator will be the first).
@Component
public class ChatSocketHandler extends TextWebSocketHandler {
    private final ChatSessions sessions;

    ChatSocketHandler(ChatSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(userId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(userId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // ignored for now
    }

    private static UUID userId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(TokenHandshakeInterceptor.USER_ID);
    }
}
