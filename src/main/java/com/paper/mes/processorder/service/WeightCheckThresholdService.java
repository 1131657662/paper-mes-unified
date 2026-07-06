package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.WeightCheckCalculator;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeightCheckThresholdService {

    public static final String WARN_KEY = "process.weightTolerancePercent";
    public static final String BLOCK_KEY = "process.weightBlockTolerancePercent";

    private static final BigDecimal DEFAULT_WARN = new BigDecimal("3");
    private static final BigDecimal DEFAULT_BLOCK = new BigDecimal("5");

    private final SystemConfigService systemConfigService;

    public WeightCheckCalculator.Thresholds currentThresholds() {
        Map<String, SysConfigItem> configs = systemConfigService.enabledByKeys(List.of(WARN_KEY, BLOCK_KEY))
                .stream()
                .collect(Collectors.toMap(SysConfigItem::getConfigKey, Function.identity(), (left, right) -> left));
        BigDecimal warn = configDecimal(configs.get(WARN_KEY), DEFAULT_WARN);
        BigDecimal block = configDecimal(configs.get(BLOCK_KEY), DEFAULT_BLOCK);
        return thresholds(warn, block);
    }

    private WeightCheckCalculator.Thresholds thresholds(BigDecimal warn, BigDecimal block) {
        try {
            return WeightCheckCalculator.Thresholds.of(warn, block);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("重量闭合阈值配置不正确：拦截阈值不能小于警告阈值");
        }
    }

    private BigDecimal configDecimal(SysConfigItem item, BigDecimal fallback) {
        if (item == null || !StringUtils.hasText(item.getConfigValue())) {
            return fallback;
        }
        try {
            return new BigDecimal(item.getConfigValue().trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("重量闭合阈值配置不正确：" + item.getConfigKey());
        }
    }
}
