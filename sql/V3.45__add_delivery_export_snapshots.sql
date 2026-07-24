SET SESSION lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `sys_export_snapshot` (
  `uuid` VARCHAR(36) NOT NULL,
  `task_uuid` VARCHAR(36) NOT NULL,
  `snapshot_type` VARCHAR(30) NOT NULL,
  `captured_at` DATETIME NOT NULL,
  `row_count` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_export_snapshot_task` (`task_uuid`),
  KEY `idx_export_snapshot_type_time` (`snapshot_type`, `captured_at`, `uuid`),
  CONSTRAINT `fk_export_snapshot_task` FOREIGN KEY (`task_uuid`)
    REFERENCES `sys_export_task` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_export_snapshot_row_count` CHECK (`row_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出数据快照';

CREATE TABLE IF NOT EXISTS `sys_export_snapshot_row` (
  `snapshot_uuid` VARCHAR(36) NOT NULL,
  `row_no` BIGINT NOT NULL,
  `row_payload` JSON NOT NULL,
  PRIMARY KEY (`snapshot_uuid`, `row_no`),
  CONSTRAINT `fk_export_snapshot_row_snapshot` FOREIGN KEY (`snapshot_uuid`)
    REFERENCES `sys_export_snapshot` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_export_snapshot_row_no` CHECK (`row_no` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出快照明细';
