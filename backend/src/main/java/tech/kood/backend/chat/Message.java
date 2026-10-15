package tech.kood.backend.chat;

import java.time.Instant;
import java.util.UUID;

public record Message(long id, UUID senderId, String body, Instant createdAt) {}
