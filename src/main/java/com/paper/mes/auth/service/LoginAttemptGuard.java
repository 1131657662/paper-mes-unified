package com.paper.mes.auth.service;

import com.paper.mes.auth.config.AuthProperties;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class LoginAttemptGuard {

    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private final AuthProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptGuard(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginAttemptGuard(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void ensureAllowed(String clientId) {
        String key = clientKey(clientId);
        AttemptState state = attempts.get(key);
        if (state == null) return;
        Instant now = clock.instant();
        if (state.isBlocked(now)) throw tooManyAttempts();
        if (state.isExpired(now, properties.getLoginWindowSeconds())) attempts.remove(key, state);
    }

    public void recordFailure(String clientId) {
        String key = clientKey(clientId);
        ensureCapacity(key);
        AttemptState state = attempts.compute(key, (ignored, current) -> nextFailure(current));
        log.warn("Failed login attempt from client {}, count {}", key, state.failures());
    }

    public void recordSuccess(String clientId) {
        attempts.remove(clientKey(clientId));
    }

    private AttemptState nextFailure(AttemptState current) {
        Instant now = clock.instant();
        if (current == null || current.isExpired(now, properties.getLoginWindowSeconds())) {
            return new AttemptState(1, now, null);
        }
        int failures = current.failures() + 1;
        Instant blockedUntil = failures >= properties.getLoginMaxFailures()
                ? now.plusSeconds(properties.getLoginLockSeconds()) : null;
        return new AttemptState(failures, current.windowStartedAt(), blockedUntil);
    }

    private void ensureCapacity(String key) {
        if (attempts.containsKey(key) || attempts.size() < MAX_TRACKED_CLIENTS) return;
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> entry.getValue()
                .isExpired(now, properties.getLoginWindowSeconds()));
        if (attempts.size() >= MAX_TRACKED_CLIENTS) throw tooManyAttempts();
    }

    private String clientKey(String clientId) {
        return clientId == null || clientId.isBlank() ? "unknown" : clientId.trim();
    }

    private BusinessException tooManyAttempts() {
        return new BusinessException(ResultCode.TOO_MANY_REQUESTS, "登录失败次数过多，请稍后重试");
    }

    private record AttemptState(int failures, Instant windowStartedAt, Instant blockedUntil) {
        boolean isBlocked(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        boolean isExpired(Instant now, int windowSeconds) {
            if (blockedUntil != null) return !now.isBefore(blockedUntil);
            return !now.isBefore(windowStartedAt.plusSeconds(windowSeconds));
        }
    }
}
