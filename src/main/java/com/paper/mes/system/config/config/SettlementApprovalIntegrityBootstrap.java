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
public class SettlementApprovalIntegrityBootstrap implements ApplicationRunner {
    private static final String SETTLE_TABLE = "biz_settle_order";
    private static final String RECEIVE_TABLE = "biz_receive_record";
    private static final String APPROVAL_TABLE = "biz_settle_discount_approval";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addSettleColumns();
        addReceiveColumns();
        createApprovalTable();
        addRequestIndex();
    }

    private void addSettleColumns() {
        addColumn(SETTLE_TABLE, "request_id", "VARCHAR(64) DEFAULT NULL COMMENT '客户端幂等请求号' AFTER `customer_name`");
        addColumn(SETTLE_TABLE, "quote_version", "VARCHAR(32) DEFAULT NULL COMMENT '创建时报价算法版本' AFTER `request_id`");
        addColumn(SETTLE_TABLE, "quote_hash", "CHAR(64) DEFAULT NULL COMMENT '创建时报价SHA-256' AFTER `quote_version`");
    }

    private void addReceiveColumns() {
        addColumn(RECEIVE_TABLE, "discount_reason", "VARCHAR(255) DEFAULT NULL COMMENT '优惠及尾差核销原因' AFTER `discount_amount`");
        addColumn(RECEIVE_TABLE, "discount_approval_uuid", "VARCHAR(36) DEFAULT NULL COMMENT '超过阈值时关联审批记录' AFTER `discount_reason`");
        addColumn(RECEIVE_TABLE, "discount_approved_by", "VARCHAR(50) DEFAULT NULL COMMENT '优惠批准人或免审登记人' AFTER `discount_approval_uuid`");
    }

    private void addRequestIndex() {
        if (!indexExists(SETTLE_TABLE, "uk_settle_request_id")) {
            jdbcTemplate.execute("ALTER TABLE `biz_settle_order` ADD UNIQUE KEY `uk_settle_request_id` (`request_id`)");
        }
    }

    private void createApprovalTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `biz_settle_discount_approval` (
                  `uuid` VARCHAR(36) NOT NULL,
                  `settle_uuid` VARCHAR(36) NOT NULL,
                  `request_id` VARCHAR(64) NOT NULL,
                  `discount_amount` DECIMAL(12,2) NOT NULL,
                  `reason` VARCHAR(255) NOT NULL,
                  `approval_status` TINYINT NOT NULL DEFAULT 1,
                  `request_by` VARCHAR(36) NOT NULL,
                  `request_by_name` VARCHAR(50) NOT NULL,
                  `request_time` DATETIME NOT NULL,
                  `approve_by` VARCHAR(36) DEFAULT NULL,
                  `approve_by_name` VARCHAR(50) DEFAULT NULL,
                  `approve_time` DATETIME DEFAULT NULL,
                  `used_receive_uuid` VARCHAR(36) DEFAULT NULL,
                  `is_deleted` TINYINT NOT NULL DEFAULT 0,
                  `create_by` VARCHAR(50) DEFAULT NULL,
                  `update_by` VARCHAR(50) DEFAULT NULL,
                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` INT NOT NULL DEFAULT 1,
                  `ext_str1` VARCHAR(255) DEFAULT NULL,
                  `ext_str2` VARCHAR(255) DEFAULT NULL,
                  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
                  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_discount_approval_request` (`settle_uuid`, `request_id`),
                  UNIQUE KEY `uk_discount_approval_receive` (`used_receive_uuid`),
                  KEY `idx_discount_approval_settle_status` (`settle_uuid`, `approval_status`),
                  CONSTRAINT `chk_discount_approval_amount_positive` CHECK (`discount_amount` > 0)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算优惠及尾差审批记录'
                """);
    }

    private void addColumn(String tableName, String columnName, String definition) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }
}
