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
                rs.getShort("skill_level"),
                rs.getString("goal"),
                rs.getString("looking_for"),
                rs.getString("env_pref"),
                rs.getShort("age"),
                rs.getShort("age_min"),
                rs.getShort("age_max"),
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
}
