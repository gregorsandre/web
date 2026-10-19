package tech.kood.backend.chat;

import java.util.UUID;

record Conversation(long id, UUID userA, UUID userB) {
    boolean includes(UUID userId) {
        return userA.equals(userId) || userB.equals(userId);
    }

    UUID other(UUID userId) {
        return userA.equals(userId) ? userB : userA;
    }
}
