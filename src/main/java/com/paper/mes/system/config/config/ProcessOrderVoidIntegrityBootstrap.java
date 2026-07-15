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
@Order(34)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessOrderVoidIntegrityBootstrap implements ApplicationRunner {

    private static final String TABLE_NAME = "biz_process_order";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("void_time",
                "ALTER TABLE `biz_process_order` ADD COLUMN `void_time` DATETIME DEFAULT NULL COMMENT '作废时间' AFTER `back_record_user`");
        addColumnIfMissing("void_user",
                "ALTER TABLE `biz_process_order` ADD COLUMN `void_user` VARCHAR(50) DEFAULT NULL COMMENT '作废人' AFTER `void_time`");
        addColumnIfMissing("void_reason",
                "ALTER TABLE `biz_process_order` ADD COLUMN `void_reason` VARCHAR(255) DEFAULT NULL COMMENT '作废原因' AFTER `void_user`");
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        if (!columnExists(columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, TABLE_NAME, columnName);
        return count != null && count > 0;
    }
}
