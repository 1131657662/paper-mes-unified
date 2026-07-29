package com.paper.mes.processorder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 为本地开发数据库补齐回录实际产出字段；生产环境由版本化 SQL 迁移执行。 */
@Component
@RequiredArgsConstructor
@Order(39)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class BackRecordProductionSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumn("production_result", "TINYINT DEFAULT NULL COMMENT '1计划产出 2正常产出 3计划未产出 4实际新增产出'");
        addColumn("production_adjustment_reason", "VARCHAR(255) DEFAULT NULL COMMENT '回录产出调整原因'");
        addIndex();
    }

    private void addColumn(String column, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
                  AND column_name = ?
                """, Integer.class, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE biz_finish_roll ADD COLUMN " + column + " " + definition);
        }
    }

    private void addIndex() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
                  AND index_name = 'idx_finish_production_result'
                """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("CREATE INDEX idx_finish_production_result ON biz_finish_roll (order_uuid, production_result, finish_status)");
        }
    }
}
