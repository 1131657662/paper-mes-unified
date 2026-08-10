package com.paper.mes.observability.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RumRateLimiterTest {

    @Test
    void allow_rejectsAfterPerClientWindowLimit() {
        RumProperties properties = new RumProperties();
        properties.setMaxEventsPerMinute(2);
        properties.setMaxTrackedClients(10);
        Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);
        RumRateLimiter limiter = new RumRateLimiter(properties, clock);

        assertThat(limiter.allow("client-a")).isTrue();
        assertThat(limiter.allow("client-a")).isTrue();
        assertThat(limiter.allow("client-a")).isFalse();
    }
}
