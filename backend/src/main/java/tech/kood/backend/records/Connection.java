package tech.kood.backend.records;

import java.time.Instant;
import java.util.UUID;

public record Connection(Integer id, UUID requesterId, UUID addresseeId, String status, Instant createdAt, Instant respondedAt) {}
