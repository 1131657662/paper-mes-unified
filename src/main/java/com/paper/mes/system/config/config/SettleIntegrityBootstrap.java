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
@Order(30)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SettleIntegrityBootstrap implements ApplicationRunner {

    private static final String ORDER_TABLE = "biz_settle_order";
    private static final String RECEIVE_TABLE = "biz_receive_record";
    private static final String DETAIL_TABLE = "biz_settle_detail";
    private static final String DISCOUNT_COLUMN = "discount_amount";
    private static final String ACTIVE_ORDER_COLUMN = "active_order_uuid";
    private static final String ACTIVE_ORDER_INDEX = "uk_settle_detail_order_active";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addOrderDiscountColumn();
        addReceiveDiscountColumn();
        addActiveOrderColumn();
        addActiveOrderIndex();
    }

    private void addOrderDiscountColumn() {
        if (columnExists(ORDER_TABLE, DISCOUNT_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_settle_order`
                ADD COLUMN `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00
                  COMMENT '优惠及尾差核销金额' AFTER `scrap_offset_amount`,
                ADD CONSTRAINT `chk_settle_discount_nonnegative` CHECK (`discount_amount` >= 0)
                """);
    }

    private void addReceiveDiscountColumn() {
        if (columnExists(RECEIVE_TABLE, DISCOUNT_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_receive_record`
                ADD COLUMN `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00
                  COMMENT '优惠及尾差核销金额' AFTER `scrap_offset_amount`,
                ADD CONSTRAINT `chk_receive_discount_nonnegative` CHECK (`discount_amount` >= 0)
                """);
    }

    private void addActiveOrderColumn() {
        if (columnExists(DETAIL_TABLE, ACTIVE_ORDER_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_settle_detail`
                ADD COLUMN `active_order_uuid` VARCHAR(36)
                GENERATED ALWAYS AS (
                  CASE
                    WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`order_uuid`), '')
                    ELSE NULL
                  END
                ) STORED COMMENT 'active unique settled process order'
                """);
    }

    private void addActiveOrderIndex() {
        if (indexExists(DETAIL_TABLE, ACTIVE_ORDER_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_settle_detail`
                ADD UNIQUE KEY `uk_settle_detail_order_active` (`active_order_uuid`)
                """);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
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
