package tech.kood.backend.access;

import org.springframework.stereotype.Service;
import tech.kood.backend.NotFoundException;
import tech.kood.backend.repositories.ConnectionRepository;
import tech.kood.backend.services.RecommendationService;

import java.util.UUID;

@Service
public class AccessService {
    private final ConnectionRepository connectionRepository;
    private final RecommendationService recommendations;

    AccessService(ConnectionRepository connectionRepository, RecommendationService recommendations) {
        this.connectionRepository = connectionRepository;
        this.recommendations = recommendations;
    }

    public boolean canView(UUID viewerId, UUID targetId) {
        if (viewerId.equals(targetId)) {
            return true;
        }
        if (connectionRepository.findBetween(viewerId, targetId).isPresent()) {
            return true;
        }
        return recommendations.getRecommendationIds(viewerId).contains(targetId);
    }

    public UUID requireViewable(UUID viewerId, String rawId) {
        UUID targetId;
        if ("me".equals(rawId)) {
            targetId = viewerId;
        } else {
            try {
                targetId = UUID.fromString(rawId);
            } catch (IllegalArgumentException e) {
                throw new NotFoundException("ID not found");
            }
        }
        if (!canView(viewerId, targetId)) {
            throw new NotFoundException("Not allowed to view target");
        }
        return targetId;
    }
}


