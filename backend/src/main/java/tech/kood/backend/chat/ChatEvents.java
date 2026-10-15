package tech.kood.backend.chat;

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
}
