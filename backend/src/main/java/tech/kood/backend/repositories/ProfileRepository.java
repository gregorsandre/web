package tech.kood.backend.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tech.kood.backend.records.Profile;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ProfileRepository {
    private final JdbcTemplate jdbc;

    public ProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Profile> MAPPER = (rs, i) -> new Profile(
            rs.getObject("user_id", UUID.class),
            rs.getString("display_name"),
            rs.getString("about_me"),
            rs.getString("avatar_path"),
            rs.getBoolean("completed"),
            rs.getTimestamp("updated_at").toInstant()
    );

    public Optional<Profile> findById(UUID id) {
        return jdbc.query("SELECT * FROM profiles WHERE user_id = ?", MAPPER, id)
                .stream().findFirst();
    }
    public int saveProfile(UUID userId, String displayName, String aboutMe) {
        return jdbc.update("""
            INSERT INTO profiles (user_id, display_name, about_me, updated_at)
            VALUES (?, ?, ?, now())
            ON CONFLICT (user_id) DO UPDATE SET
                display_name = EXCLUDED.display_name,
                about_me     = EXCLUDED.about_me,
                updated_at   = now()
            """, userId, displayName, aboutMe);
    }

    public void markCompleted(UUID userId) {
        jdbc.update("UPDATE profiles SET completed = true, updated_at = now() WHERE user_id = ?", userId);
    }
}
