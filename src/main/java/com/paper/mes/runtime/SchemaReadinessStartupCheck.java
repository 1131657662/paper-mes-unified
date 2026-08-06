package com.paper.mes.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class SchemaReadinessStartupCheck implements ApplicationRunner {

    private final SchemaReadinessService readinessService;

    @Override
    public void run(ApplicationArguments args) {
        SchemaReadinessReport report = readinessService.refresh();
        if (report.ready()) {
            log.info("Database schema ready: version={}", report.databaseVersion());
            return;
        }
        log.error("Database schema not ready: expected={}, actual={}, missing={}",
                report.expectedVersion(), report.databaseVersion(), report.missingStructures());
    }
}
