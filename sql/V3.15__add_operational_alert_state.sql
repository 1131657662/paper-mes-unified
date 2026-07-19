SET SESSION lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `sys_operational_alert_state` (
  `alert_key` VARCHAR(64) NOT NULL,
  `state_code` VARCHAR(30) NOT NULL,
  `transition_no` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`alert_key`),
  CONSTRAINT `chk_operational_alert_transition_no` CHECK (`transition_no` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨实例运行态告警状态';
