package tech.kood.backend.records;

import java.util.List;

public record ProfileBioInput(
        String displayName,
        String aboutMe,
        BioInput bio,
        List<Integer> tagIds
) {}