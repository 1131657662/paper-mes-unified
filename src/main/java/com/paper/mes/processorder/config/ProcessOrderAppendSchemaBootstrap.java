package com.paper.mes.processorder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(39)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ProcessOrderAppendSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createSessionTable();
        createRollTable();
        addColumn("biz_process_order_append_session", "commit_request_id",
                "ALTER TABLE `biz_process_order_append_session` ADD COLUMN `commit_request_id` varchar(64) DEFAULT NULL AFTER `operator`");
        addColumn("biz_process_order_append_session", "active_order_uuid",
                "ALTER TABLE `biz_process_order_append_session` ADD COLUMN `active_order_uuid` varchar(36) "
                        + "GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 AND `status` IN ('DRAFT','READY') "
                        + "THEN `order_uuid` ELSE NULL END) STORED AFTER `is_deleted`");
        addIndex("biz_process_order_append_session", "uk_process_append_active_order",
                "ALTER TABLE `biz_process_order_append_session` ADD UNIQUE KEY "
                        + "`uk_process_append_active_order` (`active_order_uuid`)");
        addBaseExtensionColumns("biz_process_order_append_session");
        addBaseExtensionColumns("biz_process_order_append_roll");
        addColumn("biz_process_order_append_roll", "weight_status",
                "ALTER TABLE `biz_process_order_append_roll` ADD COLUMN `weight_status` varchar(16) DEFAULT 'ESTIMATED' AFTER `roll_weight`");
        jdbcTemplate.execute("ALTER TABLE `biz_process_order_append_roll` MODIFY COLUMN `roll_weight` decimal(12,3) DEFAULT NULL");
        addColumn("biz_process_order_append_roll", "service_steps_json",
                "ALTER TABLE `biz_process_order_append_roll` ADD COLUMN `service_steps_json` json DEFAULT NULL AFTER `config_type`");
    }

    private void createSessionTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `biz_process_order_append_session` (
                  `uuid` varchar(36) NOT NULL,
                  `order_uuid` varchar(36) NOT NULL,
                  `session_no` varchar(64) NOT NULL,
                  `base_order_version` int NOT NULL,
                  `status` varchar(16) NOT NULL DEFAULT 'DRAFT',
                  `reason` varchar(255) DEFAULT NULL,
                  `operator` varchar(100) DEFAULT NULL,
                  `commit_request_id` varchar(64) DEFAULT NULL,
                  `apply_time` datetime DEFAULT NULL,
                  `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `active_order_uuid` varchar(36) GENERATED ALWAYS AS
                    (CASE WHEN `is_deleted` = 0 AND `status` IN ('DRAFT','READY')
                      THEN `order_uuid` ELSE NULL END) STORED,
                  `create_by` varchar(50) DEFAULT NULL,
                  `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1,
                  `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL,
                  `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_process_append_session_no` (`session_no`),
                  UNIQUE KEY `uk_process_append_active_order` (`active_order_uuid`),
                  KEY `idx_process_append_order_status` (`order_uuid`,`status`,`is_deleted`),
                  CONSTRAINT `fk_process_append_session_order` FOREIGN KEY (`order_uuid`)
                    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT `chk_process_append_session_status` CHECK
                    (`status` IN ('DRAFT','READY','APPLIED','CANCELLED','EXPIRED'))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单追加母卷会话'
                """);
    }

    private void createRollTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `biz_process_order_append_roll` (
                  `uuid` varchar(36) NOT NULL,
                  `session_uuid` varchar(36) NOT NULL,
                  `row_sort` int NOT NULL,
                  `extra_no` varchar(100) DEFAULT NULL,
                  `roll_no` varchar(100) DEFAULT NULL,
                  `paper_name` varchar(100) NOT NULL,
                  `gram_weight` int NOT NULL,
                  `original_width` int NOT NULL,
                  `original_diameter` int DEFAULT NULL,
                  `core_diameter` int DEFAULT NULL,
                  `original_length` int DEFAULT NULL,
                  `roll_weight` decimal(12,3) DEFAULT NULL,
                  `weight_status` varchar(16) DEFAULT 'ESTIMATED',
                  `piece_num` int NOT NULL DEFAULT 1,
                  `batch_no` varchar(100) DEFAULT NULL,
                  `damage_desc` varchar(255) DEFAULT NULL,
                  `process_mode` tinyint DEFAULT NULL,
                  `main_step_type` tinyint DEFAULT NULL,
                  `machine_uuid` varchar(36) DEFAULT NULL,
                  `remark` varchar(255) DEFAULT NULL,
                  `config_json` json DEFAULT NULL,
                  `preview_json` json DEFAULT NULL,
                  `config_status` tinyint NOT NULL DEFAULT 0,
                  `config_type` varchar(20) DEFAULT 'singlePlan',
                  `service_steps_json` json DEFAULT NULL,
                  `last_error` varchar(500) DEFAULT NULL,
                  `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `create_by` varchar(50) DEFAULT NULL,
                  `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1,
                  `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL,
                  `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_process_append_roll_sort` (`session_uuid`,`row_sort`,`is_deleted`),
                  KEY `idx_process_append_roll_session` (`session_uuid`,`is_deleted`),
                  CONSTRAINT `fk_process_append_roll_session` FOREIGN KEY (`session_uuid`)
                    REFERENCES `biz_process_order_append_session` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT,
                  CONSTRAINT `chk_process_append_roll_config_status` CHECK (`config_status` IN (0,1))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单追加母卷草稿'
                """);
    }

    private void addBaseExtensionColumns(String table) {
        addColumn(table, "ext_str1", "ALTER TABLE `" + table
                + "` ADD COLUMN `ext_str1` varchar(255) DEFAULT NULL AFTER `version`");
        addColumn(table, "ext_str2", "ALTER TABLE `" + table
                + "` ADD COLUMN `ext_str2` varchar(255) DEFAULT NULL AFTER `ext_str1`");
        addColumn(table, "ext_num1", "ALTER TABLE `" + table
                + "` ADD COLUMN `ext_num1` decimal(12,3) DEFAULT NULL AFTER `ext_str2`");
        addColumn(table, "ext_num2", "ALTER TABLE `" + table
                + "` ADD COLUMN `ext_num2` decimal(12,3) DEFAULT NULL AFTER `ext_num1`");
    }

    private void addColumn(String table, String column, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndex(String table, String index, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, table, index);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
