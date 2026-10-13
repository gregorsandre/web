package tech.kood.backend.recommendation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kood.backend.auth.User;

import java.util.List;
import java.util.UUID;

@RestController
public class RecommendationController {
    private final RecommendationService service;

    RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recommendations")
    List<UUID> getRecommendations(@AuthenticationPrincipal User me) {
        return service.getRecommendationIds(me.id());
    }
}