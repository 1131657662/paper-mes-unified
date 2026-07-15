package com.paper.mes.backup.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(0)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupIntegrityBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_backup_task` (
                  `uuid` varchar(36) NOT NULL,
                  `task_type` varchar(20) NOT NULL,
                  `backup_id` varchar(15) DEFAULT NULL,
                  `task_status` varchar(20) NOT NULL,
                  `started_at` datetime NOT NULL,
                  `finished_at` datetime DEFAULT NULL,
                  `duration_ms` bigint DEFAULT NULL,
                  `operator` varchar(50) NOT NULL,
                  `message` varchar(255) DEFAULT NULL,
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
                  KEY `idx_backup_task_started` (`started_at`),
                  KEY `idx_backup_task_status` (`task_status`, `started_at`),
                  KEY `idx_backup_task_backup` (`backup_id`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备份与恢复演练任务记录'
                """);
    }
}
