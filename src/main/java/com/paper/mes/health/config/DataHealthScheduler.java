package com.paper.mes.health.config;

import com.paper.mes.health.service.DataHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataHealthScheduler {

    private final DataHealthService dataHealthService;

    @Scheduled(
            initialDelayString = "${app.data-health.initial-delay-ms:60000}",
            fixedDelayString = "${app.data-health.scan-delay-ms:900000}")
    public void inspect() {
        try {
            dataHealthService.inspect();
        } catch (RuntimeException ex) {
            log.error("Scheduled data health inspection failed", ex);
        }
    }
}
