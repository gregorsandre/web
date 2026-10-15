package tech.kood.backend.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kood.backend.services.RecommendationService;

import java.util.List;
import java.util.UUID;

@RestController
public class RecommendationController {
    private final RecommendationService service;

    RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recommendations")
    List<UUID> getRecommendations(@AuthenticationPrincipal UUID me) {
        return service.getRecommendationIds(me);
    }
}