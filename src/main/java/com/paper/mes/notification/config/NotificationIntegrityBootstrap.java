package com.paper.mes.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(50)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationIntegrityBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_notification` (
                  `uuid` varchar(36) NOT NULL,
                  `recipient_uuid` varchar(36) NOT NULL,
                  `notification_type` varchar(30) NOT NULL,
                  `severity` varchar(10) NOT NULL,
                  `title` varchar(100) NOT NULL,
                  `content` varchar(500) NOT NULL,
                  `source_type` varchar(30) NOT NULL,
                  `source_uuid` varchar(36) NOT NULL,
                  `read_at` datetime DEFAULT NULL,
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
                  UNIQUE KEY `uk_notification_source` (`recipient_uuid`, `notification_type`, `source_uuid`),
                  KEY `idx_notification_recipient_time` (`recipient_uuid`, `create_time`),
                  KEY `idx_notification_recipient_read` (`recipient_uuid`, `read_at`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统站内通知'
                """);
    }
}
