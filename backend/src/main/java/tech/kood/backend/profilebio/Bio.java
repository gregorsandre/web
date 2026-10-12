package tech.kood.backend.profilebio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Bio(UUID userId, Integer skillLevel, String goal, String lookingFor, String envPref, Integer age, Integer ageMin, Integer ageMax, Double lat, Double lng, Integer maxRadiusKm, List<String> availability, Instant updatedAt) {}