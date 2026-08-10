package com.paper.mes.observability.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Small in-memory guard for the intentionally low-volume anonymous collector. */
@Component
public class RumRateLimiter {

    private static final int WINDOW_SECONDS = 60;
    private final RumProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public RumRateLimiter(RumProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RumRateLimiter(RumProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean allow(String clientId) {
        String key = normalize(clientId);
        Instant now = clock.instant();
        Window current = windows.get(key);
        if (current != null && !expired(current, now)) {
            if (current.count() >= properties.getMaxEventsPerMinute()) return false;
            return windows.replace(key, current, new Window(current.startedAt(), current.count() + 1));
        }
        ensureCapacity(now);
        if (windows.size() >= properties.getMaxTrackedClients() && !windows.containsKey(key)) return false;
        Window replacement = new Window(now, 1);
        Window previous = windows.putIfAbsent(key, replacement);
        if (previous == null || expired(previous, now)) {
            if (previous != null) windows.replace(key, previous, replacement);
            return true;
        }
        return allow(key);
    }

    int trackedClients() {
        return windows.size();
    }

    private void ensureCapacity(Instant now) {
        if (windows.size() < properties.getMaxTrackedClients()) return;
        windows.entrySet().removeIf(entry -> expired(entry.getValue(), now));
    }

    private boolean expired(Window window, Instant now) {
        return !now.isBefore(window.startedAt().plusSeconds(WINDOW_SECONDS));
    }

    private String normalize(String clientId) {
        return clientId == null || clientId.isBlank() ? "unknown" : clientId.trim();
    }

    private record Window(Instant startedAt, int count) {
    }
}
