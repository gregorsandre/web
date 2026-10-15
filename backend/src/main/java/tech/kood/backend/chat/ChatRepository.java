package tech.kood.backend.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ChatRepository {
    private final JdbcTemplate jdbc;

    ChatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Message> MESSAGE = (rs, i) -> new Message(
            rs.getLong("id"),
            rs.getObject("sender_id", UUID.class),
            rs.getString("body"),
            rs.getTimestamp("created_at").toInstant());

    private static final RowMapper<ChatSummary> SUMMARY = (rs, i) -> new ChatSummary(
            rs.getLong("id"),
            rs.getObject("other_id", UUID.class),
            rs.getTimestamp("last_message_at").toInstant(),
            rs.getInt("unread"));

    // Shared by the list and the single chat: the other user and how many of their messages are unread
    private static final String SUMMARY_COLUMNS = """
            SELECT c.id,
                   CASE WHEN c.user_a = ? THEN c.user_b ELSE c.user_a END AS other_id,
                   c.last_message_at,
                   (SELECT count(*) FROM messages m
                    WHERE m.conversation_id = c.id AND m.sender_id <> ? AND m.read_at IS NULL) AS unread
            FROM conversations c
            """;

    public Optional<Conversation> findConversation(long id) {
        return jdbc.query("SELECT id, user_a, user_b FROM conversations WHERE id = ?",
                (rs, i) -> new Conversation(rs.getLong("id"),
                        rs.getObject("user_a", UUID.class),
                        rs.getObject("user_b", UUID.class)),
                id).stream().findFirst();
    }

    // user_a must be the smaller uuid in Postgres' ordering, so the database picks the order
    // (Java's UUID.compareTo orders differently)
    public long findOrCreateConversation(UUID x, UUID y) {
        jdbc.update("""
                INSERT INTO conversations (user_a, user_b)
                VALUES (LEAST(?::uuid, ?::uuid), GREATEST(?::uuid, ?::uuid))
                ON CONFLICT (user_a, user_b) DO NOTHING""", x, y, x, y);
        return jdbc.queryForObject("""
                SELECT id FROM conversations
                WHERE user_a = LEAST(?::uuid, ?::uuid) AND user_b = GREATEST(?::uuid, ?::uuid)""",
                Long.class, x, y, x, y);
    }

    // Chats with at least one message, with users the viewer is still connected to, newest first
    public List<ChatSummary> findChats(UUID me) {
        return jdbc.query(SUMMARY_COLUMNS + """
                WHERE (c.user_a = ? OR c.user_b = ?)
                  AND EXISTS (SELECT 1 FROM messages m WHERE m.conversation_id = c.id)
                  AND EXISTS (SELECT 1 FROM connections k
                              WHERE k.status = 'accepted'
                                AND LEAST(k.requester_id, k.addressee_id) = c.user_a
                                AND GREATEST(k.requester_id, k.addressee_id) = c.user_b)
                ORDER BY c.last_message_at DESC, c.id DESC""",
                SUMMARY, me, me, me, me);
    }

    public ChatSummary findChat(UUID me, long chatId) {
        return jdbc.queryForObject(SUMMARY_COLUMNS + "WHERE c.id = ?", SUMMARY, me, me, chatId);
    }

    // Newest first; `before` is the id of the oldest message the client already has
    public List<Message> findMessages(long chatId, Long before, int limit) {
        if (before == null) {
            return jdbc.query("""
                    SELECT id, sender_id, body, created_at FROM messages
                    WHERE conversation_id = ? ORDER BY id DESC LIMIT ?""",
                    MESSAGE, chatId, limit);
        }
        return jdbc.query("""
                SELECT id, sender_id, body, created_at FROM messages
                WHERE conversation_id = ? AND id < ? ORDER BY id DESC LIMIT ?""",
                MESSAGE, chatId, before, limit);
    }

    // One statement, so the message and the chat's last_message_at are saved together
    public Message insertMessage(long chatId, UUID senderId, String body) {
        return jdbc.queryForObject("""
                WITH m AS (
                    INSERT INTO messages (conversation_id, sender_id, body) VALUES (?, ?, ?)
                    RETURNING id, sender_id, body, created_at
                ), c AS (
                    UPDATE conversations SET last_message_at = (SELECT created_at FROM m) WHERE id = ?
                )
                SELECT * FROM m""",
                MESSAGE, chatId, senderId, body, chatId);
    }

    public void markRead(long chatId, UUID readerId) {
        jdbc.update("""
                UPDATE messages SET read_at = now()
                WHERE conversation_id = ? AND sender_id <> ? AND read_at IS NULL""",
                chatId, readerId);
    }
}
