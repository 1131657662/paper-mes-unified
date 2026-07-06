package com.paper.mes.system.config.config;

import com.paper.mes.system.config.constant.NoRuleBizType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Order(10)
public class SystemNoRuleBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createTableIfMissing();
        createSequenceTableIfMissing();
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

    private void createSequenceTableIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_roll_no_sequence` (
                  `sequence_key` varchar(80) NOT NULL,
                  `current_value` bigint NOT NULL DEFAULT 0,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`sequence_key`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='document number sequence'
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
        List<RuleText> existing = jdbcTemplate.query("""
                SELECT rule_name, remark
                FROM sys_no_rule
                WHERE biz_type=?
                  AND is_deleted=0
                LIMIT 1
                """, (rs, rowNum) -> new RuleText(rs.getString("rule_name"), rs.getString("remark")), bizType);
        if (!existing.isEmpty()) {
            repairRuleTextIfNeeded(existing.get(0), bizType, ruleName, remark);
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

    private void repairRuleTextIfNeeded(RuleText existing, String bizType, String ruleName, String remark) {
        String nextRuleName = shouldRepairText(existing.ruleName()) ? ruleName : existing.ruleName();
        String nextRemark = shouldRepairText(existing.remark()) ? remark : existing.remark();
        if (Objects.equals(nextRuleName, existing.ruleName()) && Objects.equals(nextRemark, existing.remark())) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE sys_no_rule
                SET rule_name = ?,
                    remark = ?,
                    update_by = 'system'
                WHERE biz_type = ?
                  AND is_deleted = 0
                """, nextRuleName, nextRemark, bizType);
    }

    private boolean shouldRepairText(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        return value.contains("�")
                || value.contains("锟")
                || value.contains("Ã")
                || value.contains("Â")
                || value.contains("æ")
                || value.contains("å")
                || value.contains("ç")
                || suspiciousChineseCount(value) >= 2;
    }

    private long suspiciousChineseCount(String value) {
        return List.of("瀹", "绾", "鏈", "浠", "鍦", "彴", "簱", "鍙", "涓", "缂", "栫", "爜")
                .stream()
                .filter(value::contains)
                .count();
    }

    private record RuleText(String ruleName, String remark) {
    }
}
