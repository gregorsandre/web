package tech.kood.backend.recommendation;

import java.util.UUID;

public record Recommendation(UUID userId, Integer score, Double distance) {}