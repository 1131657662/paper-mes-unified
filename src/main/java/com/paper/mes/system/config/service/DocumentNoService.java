package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.entity.SysNoRule;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentNoService {

    private static final int PATTERN_PREFIX_DATE_SERIAL = 1;
    private static final int RESET_NONE = 0;
    private static final int RESET_DAY = 1;
    private static final int RESET_MONTH = 2;
    private static final int RESET_YEAR = 3;

    private final NoRuleService noRuleService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public String next(String bizType, LocalDate bizDate) {
        SysNoRule rule = noRuleService.activeRule(bizType);
        String sequenceKey = sequenceKey(rule, bizDate);
        long next = nextSequence(sequenceKey, rule, bizDate);
        return format(rule, bizDate, next);
    }

    public String preview(SysNoRule rule, LocalDate bizDate, long nextValue) {
        return format(rule, bizDate, nextValue);
    }

    public String sequenceKey(SysNoRule rule, LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;
        return switch (rule.getResetCycle() == null ? RESET_DAY : rule.getResetCycle()) {
            case RESET_NONE -> rule.getBizType();
            case RESET_MONTH -> rule.getBizType() + ":" + date.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case RESET_YEAR -> rule.getBizType() + ":" + date.format(DateTimeFormatter.ofPattern("yyyy"));
            case RESET_DAY -> rule.getBizType() + ":" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            default -> throw new BusinessException("单号重置周期不正确");
        };
    }

    public long currentValue(String sequenceKey) {
        List<Long> values = jdbcTemplate.queryForList("""
                SELECT current_value
                FROM sys_roll_no_sequence
                WHERE sequence_key = ?
                """, Long.class, sequenceKey);
        return values.isEmpty() || values.get(0) == null ? 0L : values.get(0);
    }

    private long nextSequence(String sequenceKey, SysNoRule rule, LocalDate bizDate) {
        jdbcTemplate.update("""
                INSERT INTO sys_roll_no_sequence(sequence_key, current_value)
                VALUES (?, 0)
                ON DUPLICATE KEY UPDATE sequence_key = sequence_key
                """, sequenceKey);
        Long current = jdbcTemplate.queryForObject("""
                SELECT current_value
                FROM sys_roll_no_sequence
                WHERE sequence_key = ?
                FOR UPDATE
                """, Long.class, sequenceKey);
        long next = Math.max(current == null ? 0L : current, maxPersistedValue(rule, bizDate)) + 1L;
        jdbcTemplate.update("""
                UPDATE sys_roll_no_sequence
                SET current_value = ?
                WHERE sequence_key = ?
                """, next, sequenceKey);
        return next;
    }

    private String format(SysNoRule rule, LocalDate bizDate, long sequence) {
        String prefix = rule.getPrefix() == null ? "" : rule.getPrefix();
        String serial = String.format("%0" + rule.getSerialLength() + "d", sequence);
        if (rule.getPatternType() != null && rule.getPatternType() == PATTERN_PREFIX_DATE_SERIAL) {
            return prefix + formatDate(rule, bizDate) + serial;
        }
        return prefix + serial;
    }

    private long maxPersistedValue(SysNoRule rule, LocalDate bizDate) {
        NoColumn column = noColumn(rule.getBizType());
        if (column == null) {
            return 0L;
        }
        String head = rule.getPrefix() + (rule.getPatternType() == PATTERN_PREFIX_DATE_SERIAL
                ? formatDate(rule, bizDate) : "");
        String regexp = "^" + head.replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("-", "\\-")
                .replace("_", "\\_") + "[0-9]{" + rule.getSerialLength() + ",}$";
        Long max = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(%s, ?) AS UNSIGNED)), 0)
                FROM %s
                WHERE is_deleted = 0
                  AND LEFT(%s, ?) = ?
                  AND %s REGEXP ?
                """.formatted(column.columnName(), column.tableName(), column.columnName(), column.columnName()),
                Long.class,
                head.length() + 1,
                head.length(),
                head,
                regexp);
        return max == null ? 0L : max;
    }

    private NoColumn noColumn(String bizType) {
        return switch (bizType) {
            case NoRuleBizType.PROCESS_ORDER -> new NoColumn("biz_process_order", "order_no");
            case NoRuleBizType.DELIVERY_ORDER -> new NoColumn("biz_delivery_order", "delivery_no");
            case NoRuleBizType.SETTLE_ORDER -> new NoColumn("biz_settle_order", "settle_no");
            case NoRuleBizType.FINISH_ROLL -> new NoColumn("biz_finish_roll", "finish_roll_no");
            case NoRuleBizType.CUSTOMER -> new NoColumn("sys_customer", "customer_code");
            case NoRuleBizType.PAPER -> new NoColumn("sys_paper", "paper_code");
            case NoRuleBizType.MACHINE -> new NoColumn("sys_machine", "machine_code");
            case NoRuleBizType.WAREHOUSE -> new NoColumn("sys_warehouse", "warehouse_code");
            default -> null;
        };
    }

    private String formatDate(SysNoRule rule, LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;
        String pattern = rule.getDatePattern() == null ? "yyyyMMdd" : rule.getDatePattern();
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    private record NoColumn(String tableName, String columnName) {
    }
}
