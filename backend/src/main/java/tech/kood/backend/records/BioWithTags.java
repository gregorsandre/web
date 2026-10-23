package tech.kood.backend.records;

import java.util.List;
import java.util.UUID;

public record BioWithTags(UUID userId, Integer skillLevel, String goal, String lookingFor, String envPref, Integer age, List<String> availability, List<Tag> tags) implements BioView {}