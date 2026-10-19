package tech.kood.backend.chat;

import java.time.Instant;
import java.util.UUID;

// One chat as the viewer sees it: userId is the other person, unread counts their unread messages
public record ChatSummary(long id, UUID userId, Instant lastMessageAt, int unread) {}
