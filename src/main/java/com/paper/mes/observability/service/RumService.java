package com.paper.mes.observability.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.observability.config.RumProperties;
import com.paper.mes.observability.config.RumRateLimiter;
import com.paper.mes.observability.dto.RumMetricRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RumService {

    private static final Set<String> METRICS = Set.of("CLS", "FCP", "INP", "LCP", "TTFB");
    private static final Map<String, Double> MAX_VALUES = Map.of("CLS", 10d, "FCP", 600_000d,
            "INP", 600_000d, "LCP", 600_000d, "TTFB", 600_000d);

    private final RumProperties properties;
    private final RumRateLimiter rateLimiter;

    public void record(RumMetricRequest event, String clientId) {
        if (!properties.isEnabled() || !rateLimiter.allow(clientId)) return;
        validateMetricValue(event);
        log.info("rum.metric name={} value={} rating={} route={} browser={} browserVersion={} deviceTier={} networkType={}",
                event.name(), event.value(), event.rating(), event.route(), event.browser(), event.browserVersion(),
                event.deviceTier(), event.networkType());
    }

    private void validateMetricValue(RumMetricRequest event) {
        if (!METRICS.contains(event.name()) || event.value() == null || !Double.isFinite(event.value())
                || event.value() < 0 || event.value() > MAX_VALUES.getOrDefault(event.name(), 0d)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Invalid Web Vitals metric value");
        }
    }
}
