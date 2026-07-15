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
@Order(33)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportIntegrityBootstrap implements ApplicationRunner {

    private static final String ORIGINAL_ROLL_TABLE = "biz_original_roll";
    private static final String MACHINE_UUID_INDEX = "idx_original_roll_machine_uuid";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addOriginalRollMachineIndex();
    }

    private void addOriginalRollMachineIndex() {
        if (indexExists(ORIGINAL_ROLL_TABLE, MACHINE_UUID_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_original_roll`
                ADD INDEX `idx_original_roll_machine_uuid` (`machine_uuid`)
                """);
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }
}
