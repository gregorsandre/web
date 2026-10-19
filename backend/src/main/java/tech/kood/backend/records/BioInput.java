package tech.kood.backend.records;

import java.util.List;

public record BioInput(
        Integer skillLevel,
        String goal,
        String lookingFor,
        String envPref,
        Integer age,
        Integer ageMin,
        Integer ageMax,
        Double lat,
        Double lng,
        Integer maxRadiusKm,
        List<String> availability
) {}
