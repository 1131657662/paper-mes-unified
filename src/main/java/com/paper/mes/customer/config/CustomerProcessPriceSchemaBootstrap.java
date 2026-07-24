package com.paper.mes.customer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(40)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class CustomerProcessPriceSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_customer_process_price` (
                  `uuid` varchar(36) NOT NULL, `customer_uuid` varchar(36) NOT NULL,
                  `catalog_uuid` varchar(36) NOT NULL, `billing_basis` varchar(12) NOT NULL,
                  `price` decimal(12,2) NOT NULL, `is_default` tinyint NOT NULL DEFAULT 0,
                  `is_deleted` tinyint NOT NULL DEFAULT 0, `create_by` varchar(50) DEFAULT NULL,
                  `update_by` varchar(50) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1, `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL, `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  `active_price_key` varchar(100) GENERATED ALWAYS AS
                    (CASE WHEN `is_deleted`=0 THEN CONCAT(`customer_uuid`,':',`catalog_uuid`,':',`billing_basis`) ELSE NULL END) STORED,
                  `active_default_key` varchar(80) GENERATED ALWAYS AS
                    (CASE WHEN `is_deleted`=0 AND `is_default`=1 THEN CONCAT(`customer_uuid`,':',`catalog_uuid`) ELSE NULL END) STORED,
                  PRIMARY KEY (`uuid`), UNIQUE KEY `uk_customer_process_price_active` (`active_price_key`),
                  UNIQUE KEY `uk_customer_process_price_default` (`active_default_key`),
                  KEY `idx_customer_process_price_customer` (`customer_uuid`,`is_deleted`),
                  KEY `idx_customer_process_price_catalog` (`catalog_uuid`,`is_deleted`),
                  CONSTRAINT `fk_customer_process_price_customer` FOREIGN KEY (`customer_uuid`) REFERENCES `sys_customer` (`uuid`),
                  CONSTRAINT `fk_customer_process_price_catalog` FOREIGN KEY (`catalog_uuid`) REFERENCES `sys_process_catalog` (`uuid`),
                  CONSTRAINT `chk_customer_process_price_basis` CHECK (`billing_basis` IN ('PIECE','TON','FIXED')),
                  CONSTRAINT `chk_customer_process_price_value` CHECK (`price` > 0),
                  CONSTRAINT `chk_customer_process_price_default` CHECK (`is_default` IN (0,1))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户服务工艺价格方案'
                """);
    }
}
