package tech.kood.backend.avatar;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class AvatarRepository {
    private final JdbcTemplate jdbc;

    AvatarRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record StoredAvatar(String contentType, byte[] data) {}

    Optional<StoredAvatar> find(UUID id) {
        return jdbc.query("SELECT content_type, data FROM avatars WHERE id = ?",
                (rs, i) -> new StoredAvatar(rs.getString("content_type"), rs.getBytes("data")), id)
                .stream().findFirst();
    }

    // The old row goes, so the old link stops working and the new one is a fresh id
    String replace(UUID userId, String contentType, byte[] data) {
        jdbc.update("DELETE FROM avatars WHERE user_id = ?", userId);
        UUID id = jdbc.queryForObject(
                "INSERT INTO avatars (user_id, content_type, data) VALUES (?, ?, ?) RETURNING id",
                UUID.class, userId, contentType, data);
        String path = "/avatars/" + id;
        jdbc.update("UPDATE profiles SET avatar_path = ?, updated_at = now() WHERE user_id = ?", path, userId);
        return path;
    }

    void remove(UUID userId) {
        jdbc.update("DELETE FROM avatars WHERE user_id = ?", userId);
        jdbc.update("UPDATE profiles SET avatar_path = NULL, updated_at = now() WHERE user_id = ?", userId);
    }
}
