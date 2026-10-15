package tech.kood.backend.chat;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import tech.kood.backend.access.AccessService;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Which open sockets want to know whether a user is online. A profile or chat page "watches" the
// user it shows; it gets the current status once and then every change.
@Component
public class PresenceWatchers {
    // watched user -> (socket id -> user owning that socket)
    private final Map<UUID, Map<String, UUID>> watchersOf = new ConcurrentHashMap<>();
    // socket id -> users it watches, so a closing socket can be cleaned up in one go
    private final Map<String, Set<UUID>> watchedBy = new ConcurrentHashMap<>();

    private final AccessService access;
    private final ChatSessions sessions;

    PresenceWatchers(AccessService access, ChatSessions sessions) {
        this.access = access;
        this.sessions = sessions;
    }

    // One lock per user: the snapshot for a new watcher and the user's own online/offline changes
    // happen one at a time, so a stale "offline" can never arrive after a newer "online"
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();

    private Object lockFor(UUID userId) {
        return locks.computeIfAbsent(userId, id -> new Object());
    }

    // Called when a socket opens: announces "online" if it is the user's first
    public void connected(UUID userId, WebSocketSession session) {
        synchronized (lockFor(userId)) {
            if (sessions.add(userId, session)) {
                announce(userId, true);
            }
        }
    }

    // Called when a socket closes: drops its watches and announces "offline" if it was the last
    public void disconnected(UUID userId, WebSocketSession session) {
        removeSocket(session.getId());
        synchronized (lockFor(userId)) {
            if (sessions.remove(userId, session)) {
                announce(userId, false);
            }
        }
    }

    public void watch(UUID viewer, String socketId, UUID target) {
        // Same rule as the profile endpoints; not allowed means no answer at all
        if (!mayView(viewer, target)) {
            return;
        }
        synchronized (lockFor(target)) {
            watchersOf.computeIfAbsent(target, id -> new ConcurrentHashMap<>()).put(socketId, viewer);
            watchedBy.computeIfAbsent(socketId, id -> ConcurrentHashMap.newKeySet()).add(target);
            sessions.sendToSocket(viewer, socketId, new ChatEvents.Presence(target, sessions.isOnline(target)));
        }
    }

    public void unwatch(String socketId, UUID target) {
        Set<UUID> targets = watchedBy.get(socketId);
        if (targets != null) {
            targets.remove(target);
        }
        dropWatcher(socketId, target);
    }

    public void removeSocket(String socketId) {
        Set<UUID> targets = watchedBy.remove(socketId);
        if (targets != null) {
            targets.forEach(target -> dropWatcher(socketId, target));
        }
    }

    private void announce(UUID userId, boolean online) {
        Map<String, UUID> watchers = watchersOf.get(userId);
        if (watchers == null) {
            return;
        }
        ChatEvents.Presence event = new ChatEvents.Presence(userId, online);
        watchers.forEach((socketId, viewer) -> sessions.sendToSocket(viewer, socketId, event));
    }

    private void dropWatcher(String socketId, UUID target) {
        watchersOf.computeIfPresent(target, (id, watchers) -> {
            watchers.remove(socketId);
            return watchers.isEmpty() ? null : watchers;
        });
    }

    private boolean mayView(UUID viewer, UUID target) {
        try {
            return access.canView(viewer, target);
        } catch (RuntimeException e) {
            // e.g. the viewer has no bio yet, so recommendations can't be calculated
            return false;
        }
    }
}
