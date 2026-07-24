package com.paper.mes.exporttask.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ExportSnapshotSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    void ensureTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_export_snapshot` (
                  `uuid` varchar(36) NOT NULL, `task_uuid` varchar(36) NOT NULL,
                  `snapshot_type` varchar(30) NOT NULL, `captured_at` datetime NOT NULL,
                  `row_count` bigint NOT NULL DEFAULT 0,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`uuid`), UNIQUE KEY `uk_export_snapshot_task` (`task_uuid`),
                  KEY `idx_export_snapshot_type_time` (`snapshot_type`, `captured_at`, `uuid`),
                  CONSTRAINT `fk_export_snapshot_task` FOREIGN KEY (`task_uuid`)
                    REFERENCES `sys_export_task` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT,
                  CONSTRAINT `chk_export_snapshot_row_count` CHECK (`row_count` >= 0)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出数据快照'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_export_snapshot_row` (
                  `snapshot_uuid` varchar(36) NOT NULL, `row_no` bigint NOT NULL,
                  `row_payload` json NOT NULL,
                  PRIMARY KEY (`snapshot_uuid`, `row_no`),
                  CONSTRAINT `fk_export_snapshot_row_snapshot` FOREIGN KEY (`snapshot_uuid`)
                    REFERENCES `sys_export_snapshot` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT,
                  CONSTRAINT `chk_export_snapshot_row_no` CHECK (`row_no` > 0)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出快照明细'
                """);
    }
}
