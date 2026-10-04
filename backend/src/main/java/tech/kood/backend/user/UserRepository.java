package tech.kood.backend.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> MAPPER = (rs, i) -> new User(
        rs.getObject("id", UUID.class),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getTimestamp("created_at").toInstant()
    );

    public UUID create(String email, String passwordHash) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash) VALUES (?, ?) RETURNING id",
                UUID.class, email, passwordHash);
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT * FROM users WHERE email = ?", MAPPER, email)
                .stream().findFirst();
    }

    public Optional<User> findById(UUID id) {
        return jdbc.query("SELECT * FROM users WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }
}