package com.paper.mes.auth.service;

import com.paper.mes.auth.config.AuthProperties;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptGuardTest {

    @Test
    void ensureAllowed_afterFailureLimit_blocksClient() {
        LoginAttemptGuard guard = guard(2);
        guard.recordFailure("client-a");
        guard.recordFailure("client-a");

        BusinessException error = assertThrows(BusinessException.class,
                () -> guard.ensureAllowed("client-a"));

        assertEquals(ResultCode.TOO_MANY_REQUESTS, error.getCode());
    }

    @Test
    void recordSuccess_clearsPreviousFailures() {
        LoginAttemptGuard guard = guard(2);
        guard.recordFailure("client-a");
        guard.recordSuccess("client-a");
        guard.recordFailure("client-a");

        assertDoesNotThrow(() -> guard.ensureAllowed("client-a"));
    }

    @Test
    void ensureAllowed_keepsClientsIsolated() {
        LoginAttemptGuard guard = guard(1);
        guard.recordFailure("client-a");

        assertDoesNotThrow(() -> guard.ensureAllowed("client-b"));
    }

    private LoginAttemptGuard guard(int maxFailures) {
        AuthProperties properties = new AuthProperties();
        properties.setLoginMaxFailures(maxFailures);
        properties.setLoginWindowSeconds(300);
        properties.setLoginLockSeconds(900);
        Clock clock = Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC);
        return new LoginAttemptGuard(properties, clock);
    }
}
