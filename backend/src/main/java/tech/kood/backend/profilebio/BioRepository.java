package tech.kood.backend.profilebio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BioRepository {
    private final JdbcTemplate jdbc;

    public BioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Bio> MAPPER = (rs, i) -> {
        Array arr = rs.getArray("availability");
        List<String> availability = arr == null ? List.of() : List.of((String[]) arr.getArray());
        return new Bio(
                rs.getObject("user_id", UUID.class),
                rs.getInt("skill_level"),
                rs.getString("goal"),
                rs.getString("looking_for"),
                rs.getString("env_pref"),
                rs.getInt("age"),
                rs.getInt("age_min"),
                rs.getInt("age_max"),
                rs.getDouble("lat"),
                rs.getDouble("lng"),
                rs.getInt("max_radius_km"),
                availability,
                rs.getTimestamp("updated_at").toInstant()
        );
    };

    public Optional<Bio> findById(UUID id) {
        return jdbc.query("SELECT * FROM bios WHERE user_id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public List<Bio> findRecommendations(UUID id) {
        return jdbc.query("""
                SELECT * FROM bios WHERE user_id <> ? AND
                NOT EXISTS (SELECT 1 FROM dismissals
                WHERE dismissals.user_id = ? AND dismissals.dismissed_user_id = bios.user_id)""",
                MAPPER, id, id);
    }

    public boolean isInRange(UUID userA, UUID userB) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM bios a, bios b
                WHERE ST_DWITHIN (a.location, b.location,
                LEAST (a.max_radius_km, b.max_radius_km) * 1000)
                AND a.user_id = ? AND b.user_id = ?)
                """, Boolean.class, userA, userB);
    }
}
