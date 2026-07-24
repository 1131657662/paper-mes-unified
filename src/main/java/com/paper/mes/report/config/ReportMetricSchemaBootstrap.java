package com.paper.mes.report.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(34)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportMetricSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ReportMetricBaselineSeeder baselineSeeder;

    @Override
    public void run(ApplicationArguments args) {
        ReportMetricSchemaSql.createStatements().forEach(jdbcTemplate::execute);
        baselineSeeder.seed();
    }
}
