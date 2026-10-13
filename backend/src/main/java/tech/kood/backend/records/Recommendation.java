package tech.kood.backend.records;

import java.util.UUID;

public record Recommendation(UUID userId, Integer score, Double distance) {}