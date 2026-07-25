package com.paper.mes.system.config.service.impl;

import com.paper.mes.common.BusinessException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Set;

final class SystemConfigValueValidator {

    private static final String SPARE_ROLL_COUNT_KEY = "process.spareRollNoCount";
    private static final String PRICING_AUTO_APPROVE_LIMIT_KEY = "process.pricingAutoApproveLimit";
    private static final String WEIGHT_WARN_KEY = "process.weightTolerancePercent";
    private static final String WEIGHT_BLOCK_KEY = "process.weightBlockTolerancePercent";
    private static final String DEFAULT_PAGE_SIZE_KEY = "ui.defaultPageSize";
    private static final String BACKUP_RETENTION_DAYS_KEY = "backup.retentionDays";
    private static final String CASH_SETTLE_BLOCK_MODE_KEY = "delivery.cashSettleBlockMode";
    private static final String DISCOUNT_AUTO_LIMIT_KEY = "settle.discountAutoApproveLimit";
    private static final String DISCOUNT_MAX_AMOUNT_KEY = "settle.discountMaxAmount";
    private static final String DISCOUNT_MAX_PERCENT_KEY = "settle.discountMaxPercent";
    private static final BigDecimal MAX_SPARE_ROLL_COUNT = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_PERCENT = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999.99");
    private static final Set<String> NUMBER_CONFIG_KEYS = Set.of(
            SPARE_ROLL_COUNT_KEY,
            PRICING_AUTO_APPROVE_LIMIT_KEY,
            WEIGHT_WARN_KEY,
            WEIGHT_BLOCK_KEY,
            DEFAULT_PAGE_SIZE_KEY,
            BACKUP_RETENTION_DAYS_KEY,
            CASH_SETTLE_BLOCK_MODE_KEY,
            DISCOUNT_AUTO_LIMIT_KEY,
            DISCOUNT_MAX_AMOUNT_KEY,
            DISCOUNT_MAX_PERCENT_KEY);

    private SystemConfigValueValidator() {
    }

    static void validate(String configKey, String configValue, String valueType) {
        if (!StringUtils.hasText(configValue)) {
            throw new BusinessException("参数值不能为空");
        }
        String value = configValue.trim();
        String type = valueType == null ? "" : valueType.trim();
        if (NUMBER_CONFIG_KEYS.contains(configKey) && !"number".equals(type)) {
            throw new BusinessException("该参数必须使用数字类型");
        }
        if ("number".equals(type)) {
            validateNumber(configKey, value);
        }
        if ("boolean".equals(type) && !isBoolean(value)) {
            throw new BusinessException("布尔参数值只能填写 true 或 false");
        }
    }

    private static void validateNumber(String configKey, String value) {
        BigDecimal number;
        try {
            number = new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException("数字参数值格式不正确");
        }
        switch (configKey) {
            case SPARE_ROLL_COUNT_KEY -> requireIntegerRange(number, BigDecimal.ZERO,
                    MAX_SPARE_ROLL_COUNT, "备用卷号数量必须是 0 到 100 的整数");
            case BACKUP_RETENTION_DAYS_KEY -> requireIntegerRange(number, BigDecimal.valueOf(7),
                    BigDecimal.valueOf(3650), "备份保留天数必须是 7 到 3650 天的整数");
            case DEFAULT_PAGE_SIZE_KEY -> requireIntegerRange(number, BigDecimal.TEN,
                    MAX_PERCENT, "默认每页条数必须是 10 到 100 的整数");
            case CASH_SETTLE_BLOCK_MODE_KEY -> requireIntegerRange(number, BigDecimal.ZERO,
                    BigDecimal.valueOf(2), "现结出库拦截模式只能是 0、1 或 2");
            case WEIGHT_WARN_KEY, WEIGHT_BLOCK_KEY, DISCOUNT_MAX_PERCENT_KEY ->
                    requireRange(number, BigDecimal.ZERO, MAX_PERCENT, "百分比参数必须在 0 到 100 之间");
            case PRICING_AUTO_APPROVE_LIMIT_KEY, DISCOUNT_AUTO_LIMIT_KEY, DISCOUNT_MAX_AMOUNT_KEY ->
                    requireMoney(number);
            default -> {
                // Generic number parameters only require a valid numeric representation.
            }
        }
    }

    private static void requireIntegerRange(BigDecimal number, BigDecimal min, BigDecimal max, String message) {
        if (number.stripTrailingZeros().scale() > 0) {
            throw new BusinessException(message);
        }
        requireRange(number, min, max, message);
    }

    private static void requireRange(BigDecimal number, BigDecimal min, BigDecimal max, String message) {
        if (number.compareTo(min) < 0 || number.compareTo(max) > 0) {
            throw new BusinessException(message);
        }
    }

    private static void requireMoney(BigDecimal number) {
        BigDecimal normalized = number.stripTrailingZeros();
        if (normalized.scale() > 2 || number.signum() < 0 || number.compareTo(MAX_MONEY) > 0) {
            throw new BusinessException("金额参数必须在 0 到 999999999.99 之间，且最多保留两位小数");
        }
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }
}
