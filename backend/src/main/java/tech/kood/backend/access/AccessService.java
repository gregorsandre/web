package tech.kood.backend.access;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.kood.backend.recommendation.RecommendationService;

import java.util.UUID;

@Service
public class AccessService {
    private final JdbcTemplate jdbc;
    private final RecommendationService recommendations;

    AccessService(JdbcTemplate jdbc, RecommendationService recommendations) {
        this.jdbc = jdbc;
        this.recommendations = recommendations;
    }

    public boolean canView(UUID viewerId, UUID targetId) {
        if (viewerId.equals(targetId)) {
            return true;
        }
        boolean linked = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM connections
                    WHERE (requester_id = ? AND addressee_id = ?)
                    OR (requester_id = ? AND addressee_id = ?)
                    )""", Boolean.class, viewerId, targetId, targetId, viewerId);
        if (Boolean.TRUE.equals(linked)) {
            return true;
        }
        return recommendations.getRecommendationsIds(viewerId).contains(targetId);
    }

    public UUID requireViewable(UUID viewerId, String rawId) {
        UUID targetId;
        if ("me".equals(rawId)) {
            targetId = viewerId;
        } else {
            try {
                targetId = UUID.fromString(rawId);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        if (!canView(viewerId, targetId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return targetId;
    }
}


