package tech.kood.backend.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tech.kood.backend.records.Tag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class TagRepository {
    private final JdbcTemplate jdbc;

    TagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Tag> MAPPER = (rs, i) -> new Tag(
            rs.getInt("id"),
            rs.getString("category"),
            rs.getString("name")
    );

    public List<Tag> findByUser(UUID userId) {
        return jdbc.query("""
                SELECT tags.id, tags.category, tags.name FROM tags
                INNER JOIN user_tags ON tags.id=user_tags.tag_id
                WHERE user_tags.user_id = ? ORDER BY tags.category, tags.name""", MAPPER, userId);
    }

    public List<Tag> findAll() {
        return jdbc.query("SELECT * FROM tags ORDER BY category, name", MAPPER);
    }

    public boolean tagsExist(List<Integer> tagIds) {
        if (tagIds.isEmpty()) {
            return false;
        }
        Set<Integer> noDuplicates = new HashSet<>(tagIds);
        int placeholderCount = noDuplicates.size();
        String placeholders = String.join(",", Collections.nCopies(placeholderCount, "?"));
        Integer tags = jdbc.queryForObject("SELECT COUNT (*) FROM tags WHERE id IN (" + placeholders + ")", Integer.class, noDuplicates.toArray());
        if (tags == placeholderCount) {
            return true;
        } else {
            return false;
        }
    }

    public Map<UUID, List<Tag>> findByUsers(List<UUID> userIds) {
        Map<UUID, List<Tag>> usersTags = new HashMap<UUID, List<Tag>>();
        if (userIds.isEmpty()) {
            return usersTags;
        }
        for (UUID user : userIds) {
            usersTags.put(user, new ArrayList<Tag>());
        }
        RowCallbackHandler handler = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                UUID user = rs.getObject("user_id", UUID.class);
                List<Tag> tags = usersTags.get(user);
                tags.add(MAPPER.mapRow(rs, 0));
            }
        };
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        jdbc.query("""
                SELECT user_tags.user_id, tags.id, tags.category, tags.name FROM tags
                INNER JOIN user_tags ON tags.id=user_tags.tag_id
                WHERE user_tags.user_id IN ("""
                + placeholders +
                """
                ) ORDER BY tags.category, tags.name""", handler, userIds.toArray());
        return usersTags;
    }

    public void replaceUserTags(UUID userId, List<Integer> tagIds) {
        Set<Integer> noDuplicateTags = new HashSet<>(tagIds);
        ArrayList<Object[]> values = new ArrayList<>();
        for (int tag : noDuplicateTags) {
            values.add(new Object[] {userId, tag});
        }
        jdbc.update("DELETE FROM user_tags WHERE user_id = ?", userId);
        jdbc.batchUpdate("INSERT INTO user_tags (user_id, tag_id) VALUES (?, ?)", values);
    }
}
