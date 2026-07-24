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
@Order(37)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportMaterializationSchemaBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ReportMaterializationRuntimeSchemaSql.createStatements().forEach(jdbcTemplate::execute);
        ReportMetricValueSchemaSql.createStatements().forEach(jdbcTemplate::execute);
        ReportSnapshotSchemaSql.createStatements().forEach(jdbcTemplate::execute);
        ReportQuerySnapshotSchemaSql.createStatements().forEach(jdbcTemplate::execute);
    }
}
