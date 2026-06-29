-- 融合版新建加工单：草稿配置与卷号序列表

CREATE TABLE IF NOT EXISTS `biz_process_config_draft` (
  `uuid` VARCHAR(36) NOT NULL COMMENT '主键UUID',
  `order_uuid` VARCHAR(36) NOT NULL COMMENT '加工单UUID',
  `original_uuid` VARCHAR(36) NOT NULL COMMENT '原纸UUID',
  `process_mode` TINYINT NOT NULL COMMENT '1标准加工 2现场定尺 3不加工直发',
  `main_step_type` TINYINT NULL COMMENT '1锯纸 2复卷',
  `config_json` JSON NOT NULL COMMENT '前端保存的单卷工艺配置',
  `preview_json` JSON NULL COMMENT '后端预览结果快照',
  `config_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未完成 1可提交',
  `last_error` VARCHAR(500) NULL COMMENT '最近一次校验错误',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
  `create_by` VARCHAR(64) NULL,
  `update_by` VARCHAR(64) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 0,
  `ext_str1` VARCHAR(255) NULL,
  `ext_str2` VARCHAR(255) NULL,
  `ext_num1` DECIMAL(18,6) NULL,
  `ext_num2` DECIMAL(18,6) NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_config_draft_roll` (`order_uuid`, `original_uuid`, `is_deleted`),
  KEY `idx_config_draft_order_status` (`order_uuid`, `config_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单单卷工艺配置草稿';

CREATE TABLE IF NOT EXISTS `sys_roll_no_sequence` (
  `sequence_key` VARCHAR(50) NOT NULL COMMENT '序列业务键',
  `current_value` BIGINT NOT NULL DEFAULT 0 COMMENT '当前已分配序号',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sequence_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局卷号序列表';
