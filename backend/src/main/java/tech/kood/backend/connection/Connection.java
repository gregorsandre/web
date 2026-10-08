package tech.kood.backend.connection;

import java.time.Instant;
import java.util.UUID;

public record Connection(Integer id, UUID requesterId, UUID addresseeId, String status, Instant createdAt, Instant respondedAt) {}
