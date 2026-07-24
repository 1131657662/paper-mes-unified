package com.paper.mes.exporttask.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(51)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExportTaskSchemaBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final ExportSnapshotSchemaInitializer snapshotSchemaInitializer;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_operational_alert_state` (
                  `alert_key` varchar(64) NOT NULL,
                  `state_code` varchar(30) NOT NULL,
                  `transition_no` bigint NOT NULL DEFAULT 0,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`alert_key`),
                  CONSTRAINT `chk_operational_alert_transition_no` CHECK (`transition_no` >= 0)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨实例运行态告警状态'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_export_task` (
                  `uuid` varchar(36) NOT NULL, `request_id` varchar(64) NOT NULL,
                  `task_type` varchar(30) NOT NULL, `module_code` varchar(30) NOT NULL DEFAULT 'settle',
                  `operation_code` varchar(50) NOT NULL DEFAULT 'detail-export',
                  `task_name` varchar(120) NOT NULL, `source_uuid` varchar(36) NOT NULL,
                  `source_path` varchar(160) DEFAULT NULL, `request_payload` text DEFAULT NULL,
                  `query_snapshot_uuid` varchar(36) DEFAULT NULL,
                  `metric_release_uuid` varchar(36) DEFAULT NULL, `requester_uuid` varchar(36) NOT NULL,
                  `requester_name` varchar(50) NOT NULL, `task_status` tinyint NOT NULL DEFAULT 1,
                  `progress` tinyint NOT NULL DEFAULT 0, `file_name` varchar(255) DEFAULT NULL,
                  `file_path` varchar(500) DEFAULT NULL, `content_type` varchar(120) DEFAULT NULL,
                  `file_size` bigint DEFAULT NULL,
                  `error_message` varchar(500) DEFAULT NULL, `started_at` datetime DEFAULT NULL,
                  `completed_at` datetime DEFAULT NULL, `acknowledged_at` datetime DEFAULT NULL,
                  `expires_at` datetime NOT NULL, `attempt_count` int NOT NULL DEFAULT 0,
                  `max_attempts` int NOT NULL DEFAULT 3, `heartbeat_at` datetime DEFAULT NULL,
                  `worker_id` varchar(100) DEFAULT NULL, `downloaded_at` datetime DEFAULT NULL,
                  `download_count` int NOT NULL DEFAULT 0, `cancelled_at` datetime DEFAULT NULL,
                  `cancelled_by` varchar(36) DEFAULT NULL, `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `create_by` varchar(50) DEFAULT NULL, `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1, `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL, `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_export_task_request` (`requester_uuid`, `request_id`),
                  KEY `idx_export_task_owner_time` (`requester_uuid`, `create_time`, `uuid`),
                  KEY `idx_export_task_owner_status` (`requester_uuid`, `task_status`, `acknowledged_at`),
                  KEY `idx_export_task_expiry` (`expires_at`, `task_status`),
                  KEY `idx_export_task_dispatch` (`task_status`, `heartbeat_at`, `create_time`),
                  KEY `idx_export_task_status_completed` (`task_status`, `completed_at`),
                  CONSTRAINT `chk_export_task_status` CHECK (`task_status` BETWEEN 1 AND 6),
                  CONSTRAINT `chk_export_task_progress` CHECK (`progress` BETWEEN 0 AND 100),
                  CONSTRAINT `chk_export_task_attempts` CHECK (`attempt_count` >= 0 AND `max_attempts` BETWEEN 1 AND 10),
                  CONSTRAINT `chk_export_task_download_count` CHECK (`download_count` >= 0)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出任务中心'
                """);
        ensureColumns();
        snapshotSchemaInitializer.ensureTables();
        ensureStatusConstraint();
        ensureIntegrityObjects();
    }

    private void ensureColumns() {
        addColumn("module_code", "varchar(30) DEFAULT 'settle' AFTER `task_type`");
        addColumn("operation_code", "varchar(50) DEFAULT 'detail-export' AFTER `module_code`");
        addColumn("source_path", "varchar(160) DEFAULT NULL AFTER `source_uuid`");
        addColumn("request_payload", "text DEFAULT NULL AFTER `source_path`");
        addColumn("query_snapshot_uuid", "varchar(36) DEFAULT NULL AFTER `request_payload`");
        addColumn("metric_release_uuid", "varchar(36) DEFAULT NULL AFTER `query_snapshot_uuid`");
        addColumn("content_type", "varchar(120) DEFAULT NULL AFTER `file_path`");
        addColumn("attempt_count", "int NOT NULL DEFAULT 0 AFTER `expires_at`");
        addColumn("max_attempts", "int NOT NULL DEFAULT 3 AFTER `attempt_count`");
        addColumn("heartbeat_at", "datetime DEFAULT NULL AFTER `max_attempts`");
        addColumn("worker_id", "varchar(100) DEFAULT NULL AFTER `heartbeat_at`");
        addColumn("downloaded_at", "datetime DEFAULT NULL AFTER `worker_id`");
        addColumn("download_count", "int NOT NULL DEFAULT 0 AFTER `downloaded_at`");
        addColumn("cancelled_at", "datetime DEFAULT NULL AFTER `download_count`");
        addColumn("cancelled_by", "varchar(36) DEFAULT NULL AFTER `cancelled_at`");
        jdbcTemplate.update("UPDATE sys_export_task SET module_code = COALESCE(module_code, 'settle'), "
                + "operation_code = COALESCE(operation_code, 'detail-export') WHERE module_code IS NULL OR operation_code IS NULL");
    }

    private void addColumn(String name, String definition) {
        if (!columnExists(name)) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD COLUMN `" + name + "` " + definition);
        }
    }

    private void ensureStatusConstraint() {
        if (statusConstraintSupportsAllStates()) return;
        if (checkExists("chk_export_task_status")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` DROP CHECK `chk_export_task_status`");
        }
        jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD CONSTRAINT `chk_export_task_status` "
                + "CHECK (`task_status` BETWEEN 1 AND 6)");
    }

    private void ensureIntegrityObjects() {
        if (!checkExists("chk_export_task_attempts")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD CONSTRAINT `chk_export_task_attempts` "
                    + "CHECK (`attempt_count` >= 0 AND `max_attempts` BETWEEN 1 AND 10)");
        }
        if (!checkExists("chk_export_task_download_count")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD CONSTRAINT `chk_export_task_download_count` "
                    + "CHECK (`download_count` >= 0)");
        }
        if (!indexExists("idx_export_task_dispatch")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_dispatch` "
                    + "(`task_status`, `heartbeat_at`, `create_time`)");
        }
        if (!indexExists("idx_export_task_status_completed")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_status_completed` "
                    + "(`task_status`, `completed_at`)");
        }
        if (!indexExists("idx_export_task_owner_module_operation_time")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_owner_module_operation_time` "
                    + "(`requester_uuid`, `module_code`, `operation_code`, `create_time`, `uuid`)");
        }
        if (!indexExists("idx_export_task_query_snapshot")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_query_snapshot` "
                    + "(`query_snapshot_uuid`)");
        }
        if (!indexExists("idx_export_task_metric_release_time")) {
            jdbcTemplate.execute("ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_metric_release_time` "
                    + "(`metric_release_uuid`, `create_time`, `uuid`)");
        }
    }

    private boolean statusConstraintSupportsAllStates() {
        List<String> clauses = jdbcTemplate.queryForList("""
                SELECT check_clause FROM information_schema.check_constraints
                WHERE constraint_schema = DATABASE() AND constraint_name = 'chk_export_task_status'
                """, String.class);
        return clauses.stream().map(value -> value.replace("`", "").replaceAll("\\s+", " ").toLowerCase())
                .anyMatch(value -> value.contains("task_status between 1 and 6"));
    }

    private boolean columnExists(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'sys_export_task' AND column_name = ?
                """, Integer.class, name);
        return count != null && count > 0;
    }

    private boolean checkExists(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = DATABASE() AND table_name = 'sys_export_task'
                  AND constraint_name = ? AND constraint_type = 'CHECK'
                """, Integer.class, name);
        return count != null && count > 0;
    }

    private boolean indexExists(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'sys_export_task' AND index_name = ?
                """, Integer.class, name);
        return count != null && count > 0;
    }
}
