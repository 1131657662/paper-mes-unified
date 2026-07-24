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
    private static final String DUE_DATE_COLUMN = "due_date";
    private static final String REQUEST_HASH_COLUMN = "request_hash";
    private static final String DUE_DATE_INDEX = "idx_settle_due_status";
    private static final String ACTIVE_ORDER_COLUMN = "active_order_uuid";
    private static final String ACTIVE_ORDER_INDEX = "uk_settle_detail_order_active";
    private static final String STANDARD_PROCESS_COLUMN = "standard_process_amount";
    private static final String PRICING_ADJUSTMENT_COLUMN = "pricing_adjustment_amount";
    private static final String PRICING_REASON_COLUMN = "pricing_adjustment_reason";
    private static final String SERVICE_AMOUNT_COLUMN = "service_amount";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addOrderDiscountColumn();
        addReceiveDiscountColumn();
        addDueDateColumn();
        addReceiveRequestHashColumn();
        addActiveOrderColumn();
        addActiveOrderIndex();
        addPricingSnapshotColumns();
        addServiceAmountColumns();
        backfillDueDates();
        addDueDateIndex();
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

    private void addDueDateColumn() {
        addColumn(ORDER_TABLE, DUE_DATE_COLUMN,
                "DATE DEFAULT NULL COMMENT '付款到期日' AFTER `settle_date`");
    }

    private void addReceiveRequestHashColumn() {
        addColumn(RECEIVE_TABLE, REQUEST_HASH_COLUMN,
                "CHAR(64) DEFAULT NULL COMMENT '收款请求载荷SHA-256' AFTER `request_id`");
    }

    private void backfillDueDates() {
        jdbcTemplate.update("""
                UPDATE biz_settle_order s
                LEFT JOIN sys_customer c ON c.uuid = s.customer_uuid AND c.is_deleted = 0
                SET s.due_date = CASE
                  WHEN s.settle_type = 2 AND c.settle_type = 2 AND c.settle_day IS NOT NULL
                    THEN CASE
                      WHEN STR_TO_DATE(CONCAT(DATE_FORMAT(COALESCE(s.period_end, s.settle_date), '%Y-%m-'),
                              LPAD(LEAST(c.settle_day, DAY(LAST_DAY(COALESCE(s.period_end, s.settle_date)))), 2, '0')),
                              '%Y-%m-%d') >= COALESCE(s.period_end, s.settle_date)
                        THEN STR_TO_DATE(CONCAT(DATE_FORMAT(COALESCE(s.period_end, s.settle_date), '%Y-%m-'),
                                LPAD(LEAST(c.settle_day, DAY(LAST_DAY(COALESCE(s.period_end, s.settle_date)))), 2, '0')),
                                '%Y-%m-%d')
                      ELSE DATE_ADD(DATE_FORMAT(DATE_ADD(COALESCE(s.period_end, s.settle_date), INTERVAL 1 MONTH), '%Y-%m-01'),
                              INTERVAL LEAST(c.settle_day, DAY(LAST_DAY(DATE_ADD(COALESCE(s.period_end, s.settle_date), INTERVAL 1 MONTH)))) - 1 DAY)
                    END
                  ELSE s.settle_date
                END
                WHERE s.due_date IS NULL AND s.settle_date IS NOT NULL
                """);
    }

    private void addDueDateIndex() {
        if (indexExists(ORDER_TABLE, DUE_DATE_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_settle_order`
                ADD KEY `idx_settle_due_status` (`is_deleted`, `settle_status`, `due_date`, `uuid`)
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

    private void addPricingSnapshotColumns() {
        addColumn(DETAIL_TABLE, STANDARD_PROCESS_COLUMN,
                "DECIMAL(12,2) DEFAULT 0.00 COMMENT '优惠前标准加工费' AFTER `rewind_amount`");
        addColumn(DETAIL_TABLE, PRICING_ADJUSTMENT_COLUMN,
                "DECIMAL(12,2) DEFAULT 0.00 COMMENT '最终加工费减标准加工费' AFTER `standard_process_amount`");
        addColumn(DETAIL_TABLE, PRICING_REASON_COLUMN,
                "VARCHAR(255) DEFAULT NULL COMMENT '计价调整原因' AFTER `pricing_adjustment_amount`");
    }

    private void addServiceAmountColumns() {
        addColumn(ORDER_TABLE, SERVICE_AMOUNT_COLUMN,
                "DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '整理包装等服务工序费' AFTER `rewind_amount`");
        addColumn(DETAIL_TABLE, SERVICE_AMOUNT_COLUMN,
                "DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '本单服务工序费' AFTER `rewind_amount`");
    }

    private void addColumn(String tableName, String columnName, String definition) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
        }
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
