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
public class SettleCollectionSchemaBootstrap implements ApplicationRunner {
    private static final String ORDER_TABLE = "biz_settle_order";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumn("reminder_count", "INT NOT NULL DEFAULT 0 COMMENT '催收提醒次数' AFTER `unreceived_amount`");
        addColumn("last_reminder_time", "DATETIME DEFAULT NULL COMMENT '最近催收时间' AFTER `reminder_count`");
        addColumn("last_reminder_by", "VARCHAR(50) DEFAULT NULL COMMENT '最近催收人' AFTER `last_reminder_time`");
        addColumn("last_reminder_result", "TINYINT DEFAULT NULL COMMENT '最近催收结果' AFTER `last_reminder_by`");
        addColumn("next_follow_up_date", "DATE DEFAULT NULL COMMENT '下次跟进日期' AFTER `last_reminder_result`");
        addQueueIndex();
        createReminderTable();
    }

    private void createReminderTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `biz_settle_collection_reminder` (
                  `uuid` VARCHAR(36) NOT NULL,
                  `settle_uuid` VARCHAR(36) NOT NULL,
                  `request_id` VARCHAR(64) NOT NULL,
                  `reminder_channel` TINYINT NOT NULL,
                  `reminder_result` TINYINT NOT NULL,
                  `contact_name` VARCHAR(100) DEFAULT NULL,
                  `reminder_time` DATETIME NOT NULL,
                  `next_follow_up_date` DATE DEFAULT NULL,
                  `operator_uuid` VARCHAR(36) NOT NULL,
                  `operator_name` VARCHAR(50) NOT NULL,
                  `remark` VARCHAR(500) NOT NULL,
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
                  UNIQUE KEY `uk_settle_collection_request` (`settle_uuid`, `request_id`),
                  KEY `idx_settle_collection_time` (`settle_uuid`, `reminder_time`, `uuid`),
                  KEY `idx_settle_collection_follow_up` (`next_follow_up_date`, `settle_uuid`),
                  CONSTRAINT `fk_settle_collection_order` FOREIGN KEY (`settle_uuid`)
                    REFERENCES `biz_settle_order` (`uuid`) ON DELETE RESTRICT,
                  CONSTRAINT `chk_settle_collection_channel` CHECK (`reminder_channel` BETWEEN 1 AND 5),
                  CONSTRAINT `chk_settle_collection_result` CHECK (`reminder_result` BETWEEN 1 AND 5)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算催收提醒流水'
                """);
    }

    private void addColumn(String name, String definition) {
        if (!columnExists(name)) {
            jdbcTemplate.execute("ALTER TABLE `" + ORDER_TABLE + "` ADD COLUMN `" + name + "` " + definition);
        }
    }

    private void addQueueIndex() {
        if (!indexExists("idx_settle_collection_queue")) {
            jdbcTemplate.execute("""
                    ALTER TABLE `biz_settle_order` ADD INDEX `idx_settle_collection_queue`
                    (`is_deleted`, `settle_status`, `last_reminder_time`, `due_date`, `uuid`)
                    """);
        }
    }

    private boolean columnExists(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, ORDER_TABLE, name);
        return count != null && count > 0;
    }

    private boolean indexExists(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, ORDER_TABLE, name);
        return count != null && count > 0;
    }
}
