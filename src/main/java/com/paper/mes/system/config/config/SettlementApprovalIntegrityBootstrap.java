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
    private static final String SETTLE = "biz_settle_order";
    private static final String RECEIVE = "biz_receive_record";
    private static final String APPROVAL = "biz_settle_discount_approval";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addSettleColumns();
        addReceiveColumns();
        createApprovalTable();
        addApprovalColumns();
        backfillApprovalColumns();
        staleLegacyActiveApprovals();
        addIndex(SETTLE, "uk_settle_request_id", "UNIQUE KEY `uk_settle_request_id` (`request_id`)");
        addApprovalIndexes();
        addApprovalConstraints();
    }

    private void addSettleColumns() {
        addColumn(SETTLE, "request_id", "VARCHAR(64) DEFAULT NULL AFTER `customer_name`");
        addColumn(SETTLE, "quote_version", "VARCHAR(32) DEFAULT NULL AFTER `request_id`");
        addColumn(SETTLE, "quote_hash", "CHAR(64) DEFAULT NULL AFTER `quote_version`");
    }

    private void addReceiveColumns() {
        addColumn(RECEIVE, "discount_reason", "VARCHAR(255) DEFAULT NULL AFTER `discount_amount`");
        addColumn(RECEIVE, "discount_approval_uuid", "VARCHAR(36) DEFAULT NULL AFTER `discount_reason`");
        addColumn(RECEIVE, "discount_approved_by", "VARCHAR(50) DEFAULT NULL AFTER `discount_approval_uuid`");
    }

    private void addApprovalColumns() {
        addColumn(APPROVAL, "cash_amount", "DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER `request_id`");
        addColumn(APPROVAL, "scrap_offset_amount", "DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER `cash_amount`");
        addColumn(APPROVAL, "unreceived_snapshot", "DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER `discount_amount`");
        addColumn(APPROVAL, "discount_percent", "DECIMAL(7,2) NOT NULL DEFAULT 0 AFTER `unreceived_snapshot`");
        addColumn(APPROVAL, "required_level", "VARCHAR(16) NOT NULL DEFAULT 'ADMIN' AFTER `discount_percent`");
        addColumn(APPROVAL, "request_hash", "CHAR(64) NOT NULL DEFAULT '' AFTER `required_level`");
        addColumn(APPROVAL, "decision_reason", "VARCHAR(255) DEFAULT NULL AFTER `approve_time`");
        addColumn(APPROVAL, "cancel_by", "VARCHAR(36) DEFAULT NULL AFTER `decision_reason`");
        addColumn(APPROVAL, "cancel_by_name", "VARCHAR(50) DEFAULT NULL AFTER `cancel_by`");
        addColumn(APPROVAL, "cancel_time", "DATETIME DEFAULT NULL AFTER `cancel_by_name`");
        addColumn(APPROVAL, "policy_version", "VARCHAR(32) NOT NULL DEFAULT 'legacy-v1' AFTER `cancel_time`");
        addColumn(APPROVAL, "active_settle_uuid", "VARCHAR(36) GENERATED ALWAYS AS "
                + "(CASE WHEN `approval_status` IN (1,2) THEN `settle_uuid` ELSE NULL END) STORED");
    }

    private void addApprovalIndexes() {
        if (!indexExists(APPROVAL, "uk_discount_approval_active_settle")) {
            addIndex(APPROVAL, "uk_discount_approval_active_settle",
                    "UNIQUE KEY `uk_discount_approval_active_settle` (`active_settle_uuid`)");
        }
        addIndex(APPROVAL, "idx_discount_approval_inbox",
                "KEY `idx_discount_approval_inbox` (`approval_status`,`required_level`,`request_time`)");
        addIndex(APPROVAL, "idx_discount_approval_requester",
                "KEY `idx_discount_approval_requester` (`request_by`,`approval_status`,`request_time`)");
    }

    private void addApprovalConstraints() {
        addConstraint(APPROVAL, "chk_discount_approval_amount_positive",
                "CHECK (`discount_amount` > 0)");
        addConstraint(APPROVAL, "chk_discount_approval_status",
                "CHECK (`approval_status` BETWEEN 1 AND 6)");
        addConstraint(APPROVAL, "chk_discount_approval_level",
                "CHECK (`required_level` IN ('FINANCE','ADMIN'))");
        addConstraint(APPROVAL, "chk_discount_approval_components",
                "CHECK (`cash_amount` >= 0 AND `scrap_offset_amount` >= 0 "
                        + "AND `unreceived_snapshot` >= 0)");
    }

    private void backfillApprovalColumns() {
        jdbcTemplate.update("""
                UPDATE biz_settle_discount_approval a
                JOIN biz_settle_order s ON s.uuid=a.settle_uuid
                SET a.unreceived_snapshot=CASE WHEN a.unreceived_snapshot=0
                      THEN COALESCE(s.unreceived_amount,a.discount_amount) ELSE a.unreceived_snapshot END,
                    a.request_hash=CASE WHEN a.request_hash=''
                      THEN SHA2(CONCAT_WS('|',a.settle_uuid,'0.00','0.00',a.discount_amount,
                        COALESCE(s.unreceived_amount,a.discount_amount),a.reason),256) ELSE a.request_hash END,
                    a.policy_version=COALESCE(NULLIF(a.policy_version,''),'legacy-v1'),
                    a.required_level=COALESCE(NULLIF(a.required_level,''),'ADMIN')
                WHERE a.request_hash='' OR a.unreceived_snapshot=0
                   OR a.policy_version='' OR a.required_level=''
                """);
    }

    private void staleLegacyActiveApprovals() {
        jdbcTemplate.update("""
                UPDATE biz_settle_discount_approval
                SET approval_status=6,
                    decision_reason='V3.63升级：历史审批缺少完整收款方案，请重新提交'
                WHERE policy_version='legacy-v1' AND approval_status IN (1,2)
                """);
    }

    private void createApprovalTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `biz_settle_discount_approval` (
                  `uuid` VARCHAR(36) NOT NULL, `settle_uuid` VARCHAR(36) NOT NULL,
                  `request_id` VARCHAR(64) NOT NULL, `cash_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
                  `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
                  `discount_amount` DECIMAL(12,2) NOT NULL, `unreceived_snapshot` DECIMAL(12,2) NOT NULL,
                  `discount_percent` DECIMAL(7,2) NOT NULL DEFAULT 0, `required_level` VARCHAR(16) NOT NULL,
                  `request_hash` CHAR(64) NOT NULL, `reason` VARCHAR(255) NOT NULL,
                  `approval_status` TINYINT NOT NULL DEFAULT 1, `request_by` VARCHAR(36) NOT NULL,
                  `request_by_name` VARCHAR(50) NOT NULL, `request_time` DATETIME NOT NULL,
                  `approve_by` VARCHAR(36) DEFAULT NULL, `approve_by_name` VARCHAR(50) DEFAULT NULL,
                  `approve_time` DATETIME DEFAULT NULL, `decision_reason` VARCHAR(255) DEFAULT NULL,
                  `cancel_by` VARCHAR(36) DEFAULT NULL, `cancel_by_name` VARCHAR(50) DEFAULT NULL,
                  `cancel_time` DATETIME DEFAULT NULL, `policy_version` VARCHAR(32) NOT NULL,
                  `used_receive_uuid` VARCHAR(36) DEFAULT NULL,
                  `active_settle_uuid` VARCHAR(36) GENERATED ALWAYS AS
                    (CASE WHEN `approval_status` IN (1,2) THEN `settle_uuid` ELSE NULL END) STORED,
                  `is_deleted` TINYINT NOT NULL DEFAULT 0, `create_by` VARCHAR(50) DEFAULT NULL,
                  `update_by` VARCHAR(50) DEFAULT NULL, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` INT NOT NULL DEFAULT 1, `ext_str1` VARCHAR(255) DEFAULT NULL,
                  `ext_str2` VARCHAR(255) DEFAULT NULL, `ext_num1` DECIMAL(12,3) DEFAULT NULL,
                  `ext_num2` DECIMAL(12,3) DEFAULT NULL, PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_discount_approval_request` (`settle_uuid`,`request_id`),
                  UNIQUE KEY `uk_discount_approval_receive` (`used_receive_uuid`),
                  UNIQUE KEY `uk_discount_approval_active_settle` (`active_settle_uuid`),
                  KEY `idx_discount_approval_settle_status` (`settle_uuid`,`approval_status`),
                  KEY `idx_discount_approval_inbox` (`approval_status`,`required_level`,`request_time`),
                  KEY `idx_discount_approval_requester` (`request_by`,`approval_status`,`request_time`),
                  CONSTRAINT `chk_discount_approval_amount_positive` CHECK (`discount_amount` > 0),
                  CONSTRAINT `chk_discount_approval_status` CHECK (`approval_status` BETWEEN 1 AND 6),
                  CONSTRAINT `chk_discount_approval_level` CHECK (`required_level` IN ('FINANCE','ADMIN')),
                  CONSTRAINT `chk_discount_approval_components` CHECK (
                    `cash_amount` >= 0 AND `scrap_offset_amount` >= 0 AND `unreceived_snapshot` >= 0)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void addColumn(String table, String column, String definition) {
        if (!columnExists(table, column)) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        }
    }

    private void addIndex(String table, String index, String definition) {
        if (!indexExists(table, index)) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD " + definition);
        }
    }

    private void addConstraint(String table, String constraint, String definition) {
        if (!constraintExists(table, constraint)) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD CONSTRAINT `"
                    + constraint + "` " + definition);
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name=? AND column_name=?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean indexExists(String table, String index) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema=DATABASE() AND table_name=? AND index_name=?
                """, Integer.class, table, index);
        return count != null && count > 0;
    }

    private boolean constraintExists(String table, String constraint) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema=DATABASE() AND table_name=? AND constraint_name=?
                """, Integer.class, table, constraint);
        return count != null && count > 0;
    }
}
