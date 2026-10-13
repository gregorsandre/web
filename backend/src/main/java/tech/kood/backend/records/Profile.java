package tech.kood.backend.records;

import java.time.Instant;
import java.util.UUID;

public record Profile(UUID userId, String displayName, String aboutMe, String avatarPath, Boolean completed, Instant updatedAt) {}