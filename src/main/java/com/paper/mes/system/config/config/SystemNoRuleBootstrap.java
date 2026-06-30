package com.paper.mes.system.config.config;

import com.paper.mes.system.config.constant.NoRuleBizType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(10)
public class SystemNoRuleBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createTableIfMissing();
        seedRules();
    }

    private void createTableIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_no_rule` (
                  `uuid` varchar(36) NOT NULL,
                  `biz_type` varchar(50) NOT NULL,
                  `rule_name` varchar(100) NOT NULL,
                  `prefix` varchar(20) NOT NULL,
                  `pattern_type` tinyint NOT NULL DEFAULT 1,
                  `date_pattern` varchar(20) DEFAULT 'yyyyMMdd',
                  `serial_length` int NOT NULL DEFAULT 4,
                  `reset_cycle` tinyint NOT NULL DEFAULT 1,
                  `status` tinyint NOT NULL DEFAULT 1,
                  `remark` varchar(255) DEFAULT NULL,
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
                  UNIQUE KEY `uk_sys_no_rule_biz` (`biz_type`, `is_deleted`),
                  KEY `idx_sys_no_rule_status` (`status`),
                  KEY `idx_sys_no_rule_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='system document number rules'
                """);
    }

    private void seedRules() {
        seedRule("no-rule-process-order", NoRuleBizType.PROCESS_ORDER, "\u52a0\u5de5\u5355\u53f7", "JG",
                1, "yyyyMMdd", 4, 1, "\u9ed8\u8ba4\u52a0\u5de5\u5355\u53f7\uff1aJG+\u65e5\u671f+4\u4f4d\u65e5\u6d41\u6c34");
        seedRule("no-rule-delivery-order", NoRuleBizType.DELIVERY_ORDER, "\u51fa\u5e93\u5355\u53f7", "CK",
                1, "yyyyMMdd", 4, 1, "\u9ed8\u8ba4\u51fa\u5e93\u5355\u53f7\uff1aCK+\u65e5\u671f+4\u4f4d\u65e5\u6d41\u6c34");
        seedRule("no-rule-settle-order", NoRuleBizType.SETTLE_ORDER, "\u7ed3\u7b97\u5355\u53f7", "JS",
                1, "yyyyMMdd", 4, 1, "\u9ed8\u8ba4\u7ed3\u7b97\u5355\u53f7\uff1aJS+\u65e5\u671f+4\u4f4d\u65e5\u6d41\u6c34");
        seedRule("no-rule-finish-roll", NoRuleBizType.FINISH_ROLL, "\u6210\u54c1\u5377\u53f7", "A",
                2, "yyyyMMdd", 6, 0, "\u9ed8\u8ba4\u6210\u54c1\u5377\u53f7\uff1a\u524d\u7f00+6\u4f4d\u5168\u5c40\u6d41\u6c34");
        seedRule("no-rule-customer", NoRuleBizType.CUSTOMER, "\u5ba2\u6237\u7f16\u7801", "KH",
                2, "yyyyMMdd", 6, 0, "\u9ed8\u8ba4\u5ba2\u6237\u7f16\u7801\uff1aKH+6\u4f4d\u5168\u5c40\u6d41\u6c34");
        seedRule("no-rule-paper", NoRuleBizType.PAPER, "\u7eb8\u5f20\u7f16\u7801", "ZZ",
                2, "yyyyMMdd", 6, 0, "\u9ed8\u8ba4\u7eb8\u5f20\u7f16\u7801\uff1aZZ+6\u4f4d\u5168\u5c40\u6d41\u6c34");
        seedRule("no-rule-machine", NoRuleBizType.MACHINE, "\u673a\u53f0\u7f16\u7801", "JT",
                2, "yyyyMMdd", 6, 0, "\u9ed8\u8ba4\u673a\u53f0\u7f16\u7801\uff1aJT+6\u4f4d\u5168\u5c40\u6d41\u6c34");
        seedRule("no-rule-warehouse", NoRuleBizType.WAREHOUSE, "\u4ed3\u5e93\u7f16\u7801", "CKD",
                2, "yyyyMMdd", 6, 0, "\u9ed8\u8ba4\u4ed3\u5e93\u7f16\u7801\uff1aCKD+6\u4f4d\u5168\u5c40\u6d41\u6c34");
    }

    private void seedRule(String uuid, String bizType, String ruleName, String prefix,
                          int patternType, String datePattern, int serialLength,
                          int resetCycle, String remark) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_no_rule WHERE biz_type=? AND is_deleted=0",
                Integer.class,
                bizType
        );
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE sys_no_rule
                    SET rule_name = ?,
                        remark = ?,
                        update_by = 'system'
                    WHERE biz_type = ?
                      AND is_deleted = 0
                    """, ruleName, remark, bizType);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO sys_no_rule
                (uuid, biz_type, rule_name, prefix, pattern_type, date_pattern, serial_length,
                 reset_cycle, status, remark, create_by, update_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 'system', 'system')
                """, uuid, bizType, ruleName, prefix, patternType, datePattern,
                serialLength, resetCycle, remark);
    }
}
