-- V3.53: retain versioned issued snapshots and post-dispatch change audit.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_process_order_issue_versions', 10) INTO @issue_version_lock;
SET @issue_version_lock_guard_sql = IF(
  @issue_version_lock = 1,
  'DO 0',
  'SELECT V3_53_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE issue_version_lock_guard FROM @issue_version_lock_guard_sql;
EXECUTE issue_version_lock_guard;
DEALLOCATE PREPARE issue_version_lock_guard;

CREATE TABLE IF NOT EXISTS `biz_process_order_issue_version` (
  `uuid`                 VARCHAR(36)  NOT NULL COMMENT '版本历史主键',
  `order_uuid`           VARCHAR(36)  NOT NULL COMMENT '加工单 UUID',
  `version_no`           INT          NOT NULL COMMENT '业务下发版本号',
  `previous_version_no`  INT          DEFAULT NULL COMMENT '变更前业务版本号',
  `snapshot_before`      LONGTEXT     DEFAULT NULL COMMENT '变更前下发快照',
  `snapshot_after`       LONGTEXT     DEFAULT NULL COMMENT '变更后下发快照',
  `change_reason`        VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `operator_name`        VARCHAR(100) NOT NULL COMMENT '提交变更操作者',
  `change_time`          DATETIME(6)  NOT NULL COMMENT '变更提交时间',
  `issue_time`           DATETIME(6)  DEFAULT NULL COMMENT '重新下发时间',
  `issue_operator_name`  VARCHAR(100) DEFAULT NULL COMMENT '重新下发操作者',
  `status`               VARCHAR(16)  NOT NULL COMMENT 'PENDING/APPLIED/ARCHIVED',
  `is_deleted`           TINYINT      NOT NULL DEFAULT 0,
  `create_by`            VARCHAR(50)  DEFAULT NULL,
  `update_by`            VARCHAR(50)  DEFAULT NULL,
  `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version`              INT          NOT NULL DEFAULT 1,
  `ext_str1`             VARCHAR(255) DEFAULT NULL,
  `ext_str2`             VARCHAR(255) DEFAULT NULL,
  `ext_num1`             DECIMAL(12,3) DEFAULT NULL,
  `ext_num2`             DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_process_order_issue_version` (`order_uuid`, `version_no`),
  KEY `idx_process_order_issue_status` (`order_uuid`, `status`, `change_time`),
  CONSTRAINT `fk_process_order_issue_version_order`
    FOREIGN KEY (`order_uuid`) REFERENCES `biz_process_order` (`uuid`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_order_issue_version_status`
    CHECK (`status` IN ('PENDING','APPLIED','ARCHIVED')),
  CONSTRAINT `chk_process_order_issue_version_snapshot`
    CHECK ((`status` = 'PENDING' AND `snapshot_before` IS NOT NULL AND `snapshot_after` IS NULL)
      OR (`status` = 'APPLIED' AND `snapshot_after` IS NOT NULL)
      OR (`status` = 'ARCHIVED' AND `snapshot_before` IS NOT NULL)),
  CONSTRAINT `chk_process_order_issue_version_reason`
    CHECK (`status` = 'APPLIED' AND `snapshot_before` IS NULL
      OR (`change_reason` IS NOT NULL AND CHAR_LENGTH(TRIM(`change_reason`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单下发版本与变更审计';

SELECT RELEASE_LOCK('paper_mes_process_order_issue_versions') INTO @issue_version_unlock;
