package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.entity.SysNoRule;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

public final class NoRuleValidator {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern FINISH_ROLL_PREFIX_PATTERN = Pattern.compile("^[ABCDEFGHJKMNPQRSTUVWXY]$");
    private static final Set<String> DATE_PATTERNS = Set.of("yyyyMMdd", "yyyyMM", "yyyy");

    private NoRuleValidator() {
    }

    public static void validate(SysNoRule rule) {
        if (NoRuleBizType.FINISH_ROLL.equals(rule.getBizType())) {
            validateFinishRollRule(rule);
            return;
        }
        validateGenericRule(rule);
    }

    public static String normalizeDatePattern(String pattern) {
        return StringUtils.hasText(pattern) ? pattern.trim() : "yyyyMMdd";
    }

    private static void validateGenericRule(SysNoRule rule) {
        if (!StringUtils.hasText(rule.getPrefix()) || !PREFIX_PATTERN.matcher(rule.getPrefix()).matches()) {
            throw new BusinessException("单号前缀只能包含字母、数字、下划线或横线");
        }
        if (rule.getPatternType() == null || (rule.getPatternType() != 1 && rule.getPatternType() != 2)) {
            throw new BusinessException("单号格式类型不正确");
        }
        if (!DATE_PATTERNS.contains(normalizeDatePattern(rule.getDatePattern()))) {
            throw new BusinessException("日期格式仅支持 yyyyMMdd、yyyyMM、yyyy");
        }
        if (rule.getSerialLength() == null || rule.getSerialLength() < 3 || rule.getSerialLength() > 10) {
            throw new BusinessException("流水位数必须在 3 到 10 之间");
        }
        if (rule.getResetCycle() == null || rule.getResetCycle() < 0 || rule.getResetCycle() > 3) {
            throw new BusinessException("重置周期不正确");
        }
    }

    private static void validateFinishRollRule(SysNoRule rule) {
        if (!StringUtils.hasText(rule.getPrefix())
                || !FINISH_ROLL_PREFIX_PATTERN.matcher(rule.getPrefix()).matches()) {
            throw new BusinessException("成品卷号前缀必须是1位大写字母，且不能使用 I/O/L/Z");
        }
        if (rule.getPatternType() == null || rule.getPatternType() != 2) {
            throw new BusinessException("成品卷号格式必须为前缀+6位全局流水");
        }
        if (rule.getSerialLength() == null || rule.getSerialLength() != 6) {
            throw new BusinessException("成品卷号流水位数必须固定为6位");
        }
        if (rule.getResetCycle() == null || rule.getResetCycle() != 0) {
            throw new BusinessException("成品卷号必须使用全局不重置流水");
        }
    }
}
