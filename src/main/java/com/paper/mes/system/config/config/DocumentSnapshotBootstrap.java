package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentSnapshotBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("biz_delivery_order", "snap_delivery",
                "ALTER TABLE `biz_delivery_order` ADD COLUMN `snap_delivery` JSON DEFAULT NULL COMMENT 'delivery snapshot json'");
        addColumnIfMissing("biz_delivery_order", "snap_delivery_time",
                "ALTER TABLE `biz_delivery_order` ADD COLUMN `snap_delivery_time` DATETIME DEFAULT NULL COMMENT 'delivery snapshot time'");
        addColumnIfMissing("biz_settle_order", "snap_bill",
                "ALTER TABLE `biz_settle_order` ADD COLUMN `snap_bill` JSON DEFAULT NULL COMMENT 'settle bill snapshot json'");
        addColumnIfMissing("biz_settle_order", "snap_bill_time",
                "ALTER TABLE `biz_settle_order` ADD COLUMN `snap_bill_time` DATETIME DEFAULT NULL COMMENT 'settle bill snapshot time'");
    }

    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
