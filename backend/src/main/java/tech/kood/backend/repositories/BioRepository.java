package tech.kood.backend.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tech.kood.backend.records.Bio;
import tech.kood.backend.records.BioInput;

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
                        SELECT candidate.* FROM bios candidate
                        JOIN profiles cp ON candidate.user_id = cp.user_id
                        JOIN bios me ON me.user_id = ?
                        JOIN profiles mp ON mp.user_id = me.user_id
                        WHERE cp.user_id <> me.user_id
                        AND cp.completed = true
                        AND mp.completed = true
                        AND candidate.age BETWEEN me.age_min AND me.age_max
                        AND me.age BETWEEN candidate.age_min AND candidate.age_max
                        AND me.looking_for = candidate.looking_for
                        AND ST_DWITHIN (candidate.location, me.location,
                                        LEAST (candidate.max_radius_km, me.max_radius_km) * 1000)
                        AND EXISTS (SELECT 1 FROM user_tags ct
                        	JOIN user_tags mt ON ct.tag_id = mt.tag_id
                        	WHERE ct.user_id = candidate.user_id AND mt.user_id = me.user_id)
                        AND NOT EXISTS (SELECT 1 FROM dismissals
                        	WHERE dismissals.user_id = me.user_id AND dismissals.dismissed_user_id = candidate.user_id)
                        AND NOT EXISTS (SELECT 1 FROM connections c
                            WHERE (c.requester_id = candidate.user_id AND c.addressee_id = me.user_id)
                            OR (c.requester_id = me.user_id AND c.addressee_id = candidate.user_id))
                        ORDER BY ST_Distance(candidate.location, me.location)
                        LIMIT 300""",
                MAPPER, id);
    }

    public int saveBio(UUID userId, BioInput bioInput) {
        String sql = """
            INSERT INTO bios (user_id, skill_level, goal, looking_for, env_pref, age, age_min, age_max,
                              lat, lng, max_radius_km, availability, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (user_id) DO UPDATE SET
                skill_level   = EXCLUDED.skill_level,
                goal          = EXCLUDED.goal,
                looking_for   = EXCLUDED.looking_for,
                env_pref      = EXCLUDED.env_pref,
                age           = EXCLUDED.age,
                age_min       = EXCLUDED.age_min,
                age_max       = EXCLUDED.age_max,
                lat           = EXCLUDED.lat,
                lng           = EXCLUDED.lng,
                max_radius_km = EXCLUDED.max_radius_km,
                availability  = EXCLUDED.availability,
                updated_at    = now()
            """;

        return jdbc.update(sql, ps -> {
            ps.setObject(1, userId);
            ps.setInt(2, bioInput.skillLevel());
            ps.setString(3, bioInput.goal());
            ps.setString(4, bioInput.lookingFor());
            ps.setString(5, bioInput.envPref());
            ps.setInt(6, bioInput.age());
            ps.setInt(7, bioInput.ageMin());
            ps.setInt(8, bioInput.ageMax());
            ps.setDouble(9, bioInput.lat());
            ps.setDouble(10, bioInput.lng());
            ps.setInt(11, bioInput.maxRadiusKm());
            ps.setArray(12, ps.getConnection().createArrayOf("text", bioInput.availability().toArray(new String[0])));
        });
    }
}
