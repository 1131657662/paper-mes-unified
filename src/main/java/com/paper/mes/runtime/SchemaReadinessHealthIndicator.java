package com.paper.mes.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("schemaReadiness")
@RequiredArgsConstructor
public class SchemaReadinessHealthIndicator implements HealthIndicator {

    private final SchemaReadinessService readinessService;

    @Override
    public Health health() {
        SchemaReadinessReport report = readinessService.refresh();
        if (report.ready()) {
            return Health.up().withDetail("databaseVersion", report.databaseVersion()).build();
        }
        return Health.down()
                .withDetail("databaseVersion", report.databaseVersion())
                .withDetail("expectedVersion", report.expectedVersion())
                .withDetail("missingStructures", report.missingStructures())
                .build();
    }
}
