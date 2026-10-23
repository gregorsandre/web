package tech.kood.backend.records;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// The full bio plus sports, only ever shown to its owner: location and preferences stay private
public record OwnBio(UUID userId, Integer skillLevel, String goal, String lookingFor, String envPref, Integer age, Integer ageMin, Integer ageMax, Double lat, Double lng, Integer maxRadiusKm, List<String> availability, Instant updatedAt, List<Tag> tags) implements BioView {}
