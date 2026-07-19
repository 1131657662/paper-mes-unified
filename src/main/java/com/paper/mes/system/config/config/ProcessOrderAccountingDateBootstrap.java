package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps the settlement accounting-date generated column available in development databases. */
@Component
@RequiredArgsConstructor
@Order(33)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessOrderAccountingDateBootstrap implements ApplicationRunner {

    static final String TABLE = "biz_process_order";
    static final String COLUMN = "accounting_date";
    static final String INDEX = "idx_order_customer_status_accounting";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addAccountingDateColumn();
        addAccountingDateIndex();
    }

    private void addAccountingDateColumn() {
        if (columnExists()) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_process_order`
                ADD COLUMN `accounting_date` DATE GENERATED ALWAYS AS
                  (COALESCE(DATE(`back_record_time`), `order_date`)) STORED
                  COMMENT '结算归属日期'
                """);
    }

    private void addAccountingDateIndex() {
        if (indexExists()) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_process_order`
                ADD INDEX `idx_order_customer_status_accounting`
                  (`customer_uuid`, `order_status`, `accounting_date`, `uuid`)
                """);
    }

    private boolean columnExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ? AND column_name = ?
                """, Integer.class, TABLE, COLUMN);
        return count != null && count > 0;
    }

    private boolean indexExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ? AND index_name = ?
                """, Integer.class, TABLE, INDEX);
        return count != null && count > 0;
    }
}
