package com.paper.mes.system.config.service.impl;

import com.paper.mes.common.BusinessException;

import java.math.BigDecimal;

import org.springframework.util.StringUtils;

final class SystemConfigValueValidator {

    private static final String SPARE_ROLL_COUNT_KEY = "process.spareRollNoCount";
    private static final BigDecimal MAX_SPARE_ROLL_COUNT = BigDecimal.valueOf(100);

    private SystemConfigValueValidator() {
    }

    static void validate(String configKey, String configValue, String valueType) {
        if (!StringUtils.hasText(configValue)) {
            throw new BusinessException("参数值不能为空");
        }
        String value = configValue.trim();
        String type = valueType.trim();
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
        if (!SPARE_ROLL_COUNT_KEY.equals(configKey)) {
            return;
        }
        BigDecimal normalized = number.stripTrailingZeros();
        if (normalized.scale() > 0 || number.signum() < 0 || number.compareTo(MAX_SPARE_ROLL_COUNT) > 0) {
            throw new BusinessException("备用卷号数量必须是 0 到 100 的整数");
        }
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }
}
