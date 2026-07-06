package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(29)
public class ProcessDraftIntegrityBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createProcessConfigDraftTable();
    }

    private void createProcessConfigDraftTable() {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单单卷工艺配置草稿'
                """);
    }
}
