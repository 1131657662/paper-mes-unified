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
@Order(35)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SettleQueryIndexBootstrap implements ApplicationRunner {
    static final String TABLE = "biz_settle_order";
    static final String INDEX = "idx_settle_list_page";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (indexExists()) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_settle_order`
                ADD INDEX `idx_settle_list_page` (`is_deleted`, `create_time`, `uuid`)
                """);
    }

    private boolean indexExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, TABLE, INDEX);
        return count != null && count > 0;
    }
}
