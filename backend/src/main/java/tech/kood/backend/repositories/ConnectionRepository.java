package tech.kood.backend.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tech.kood.backend.records.Connection;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConnectionRepository {
    private final JdbcTemplate jdbc;

    ConnectionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Connection> MAPPER = (rs, i) -> {
        Instant responded = null;
        Timestamp response = rs.getTimestamp("responded_at");
        if (response != null) {
            responded = response.toInstant();
        }
        return new Connection(
                rs.getInt("id"),
                rs.getObject("requester_id", UUID.class),
                rs.getObject("addressee_id", UUID.class),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                responded
        );
    };

    public Optional<Connection> findBetween(UUID userA, UUID userB) {
        return jdbc.query("""
            SELECT * FROM connections
            WHERE (requester_id = ? AND addressee_id = ?)
            OR (requester_id = ? AND addressee_id = ?)""",
               MAPPER, userA, userB, userB, userA).stream().findFirst();
    }

    public List<UUID> findConnectedIds(UUID myId) {
        return jdbc.queryForList("""
                SELECT CASE
                WHEN requester_id = ? THEN addressee_id
                ELSE requester_id
                END
                FROM connections WHERE (requester_id = ? OR addressee_id = ?) AND status = 'accepted' 
                ORDER BY responded_at DESC""", UUID.class, myId, myId, myId);
    }

    public List<UUID> findRequests(UUID myId) {
        return jdbc.queryForList("""
                SELECT requester_id FROM connections
                WHERE addressee_id = ? AND status = 'pending'
                ORDER BY created_at DESC""", UUID.class, myId);
    }

    public void newRequest(UUID requesterId, UUID addresseeId) {
        jdbc.update("""
                INSERT INTO connections (requester_id, addressee_id, status, created_at)
                VALUES (?, ?, 'pending', now())""", requesterId, addresseeId);
    }

    public int acceptRequest(UUID requesterId, UUID addresseeId) {
        return jdbc.update("UPDATE connections SET status = 'accepted', responded_at = now() WHERE requester_id = ? AND addressee_id = ? AND status = 'pending'", requesterId, addresseeId);
    }

    public int deleteConnection(UUID userA, UUID userB) {
        return jdbc.update("""
            DELETE FROM connections 
            WHERE (requester_id = ? AND addressee_id = ?) 
            OR (requester_id = ? AND addressee_id = ?)""",
                userA, userB, userB, userA);
    }

    public void dismissUser(UUID me, UUID dismissed) {
        jdbc.update("""
                INSERT INTO dismissals (user_id, dismissed_user_id)
                VALUES (?, ?)""", me, dismissed);
    }
}
