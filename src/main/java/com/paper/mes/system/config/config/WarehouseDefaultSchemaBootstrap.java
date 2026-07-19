package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(36)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WarehouseDefaultSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!columnExists()) {
            jdbcTemplate.execute("""
                    ALTER TABLE `sys_warehouse`
                    ADD COLUMN `is_default` TINYINT NULL DEFAULT NULL
                    COMMENT '出库默认仓库' AFTER `status`
                    """);
            jdbcTemplate.update("UPDATE sys_warehouse SET is_default = 0 WHERE is_default IS NULL");
            jdbcTemplate.execute("""
                    ALTER TABLE `sys_warehouse`
                    MODIFY COLUMN `is_default` TINYINT NOT NULL DEFAULT 0
                    COMMENT '出库默认仓库'
                    """);
        }
        if (!indexExists()) {
            jdbcTemplate.execute("""
                    ALTER TABLE `sys_warehouse`
                    ADD INDEX `idx_warehouse_default_status` (`is_default`, `status`, `is_deleted`)
                    """);
        }
    }

    private boolean columnExists() {
        return count("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'sys_warehouse' AND column_name = 'is_default'
                """) > 0;
    }

    private boolean indexExists() {
        return count("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'sys_warehouse'
                  AND index_name = 'idx_warehouse_default_status'
                """) > 0;
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
