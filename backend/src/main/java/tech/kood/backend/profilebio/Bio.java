package tech.kood.backend.profilebio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Bio(UUID userId, Short skillLevel, String goal, String lookingFor, String envPref, Short age, Short ageMin, Short ageMax, Double lat, Double lng, Integer maxRadiusKm, List<String> availability, Instant updatedAt) {}