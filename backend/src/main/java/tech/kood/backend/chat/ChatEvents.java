package tech.kood.backend.chat;

import java.util.UUID;

// Pushed over the socket as JSON; the browser switches on "type"
public final class ChatEvents {
    private ChatEvents() {}

    public record NewMessage(String type, long chatId, Message message) {
        NewMessage(long chatId, Message message) {
            this("message", chatId, message);
        }
    }

    public record Read(String type, long chatId) {
        Read(long chatId) {
            this("read", chatId);
        }
    }

    public record Presence(String type, UUID userId, boolean online) {
        Presence(UUID userId, boolean online) {
            this("presence", userId, online);
        }
    }

    public record Typing(String type, long chatId, UUID userId) {
        Typing(long chatId, UUID userId) {
            this("typing", chatId, userId);
        }
    }
}
