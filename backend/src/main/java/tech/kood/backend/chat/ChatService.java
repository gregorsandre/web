package tech.kood.backend.chat;

import org.springframework.stereotype.Service;
import tech.kood.backend.NotFoundException;
import tech.kood.backend.repositories.ConnectionRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    static final int DEFAULT_PAGE_SIZE = 30;
    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_BODY_LENGTH = 2000;

    private final ChatRepository chats;
    private final ConnectionRepository connections;
    private final ChatSessions sessions;

    ChatService(ChatRepository chats, ConnectionRepository connections, ChatSessions sessions) {
        this.chats = chats;
        this.connections = connections;
        this.sessions = sessions;
    }

    public List<ChatSummary> listChats(UUID me) {
        return chats.findChats(me);
    }

    public ChatSummary getChat(UUID me, long chatId) {
        requireAccess(me, chatId);
        return chats.findChat(me, chatId);
    }

    public long openChat(UUID me, UUID other) {
        requireConnected(me, other);
        return chats.findOrCreateConversation(me, other);
    }

    public List<Message> messages(UUID me, long chatId, Long before, Integer limit) {
        requireAccess(me, chatId);
        int size = limit == null ? DEFAULT_PAGE_SIZE : Math.clamp(limit, 1, MAX_PAGE_SIZE);
        return chats.findMessages(chatId, before, size);
    }

    // Not @Transactional on purpose: the insert is one statement and has committed
    // before the event goes out, so a client reacting to the event can already read it
    public Message send(UUID me, long chatId, String rawBody) {
        Conversation chat = requireAccess(me, chatId);
        String body = rawBody == null ? "" : rawBody.strip();
        // Postgres text can't hold NUL characters, so they'd turn into a 500
        if (body.isEmpty() || body.indexOf('\0') >= 0 || body.codePointCount(0, body.length()) > MAX_BODY_LENGTH) {
            throw new InvalidMessageException("Message must be 1-" + MAX_BODY_LENGTH + " characters");
        }
        Message message = chats.insertMessage(chatId, me, body);
        ChatEvents.NewMessage event = new ChatEvents.NewMessage(chatId, message);
        sessions.sendTo(chat.userA(), event);
        sessions.sendTo(chat.userB(), event);
        return message;
    }

    public void markRead(UUID me, long chatId) {
        requireAccess(me, chatId);
        chats.markRead(chatId, me);
        sessions.sendTo(me, new ChatEvents.Read(chatId));
    }

    // 404 both for "no such chat" and "not yours / no longer connected", like the profile endpoints
    private Conversation requireAccess(UUID me, long chatId) {
        Conversation chat = chats.findConversation(chatId)
                .filter(c -> c.includes(me))
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        requireConnected(me, chat.other(me));
        return chat;
    }

    private void requireConnected(UUID me, UUID other) {
        boolean connected = connections.findBetween(me, other)
                .map(c -> "accepted".equals(c.status()))
                .orElse(false);
        if (!connected) {
            throw new NotFoundException("Not connected");
        }
    }
}
