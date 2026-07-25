package com.paper.mes.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SystemConfigWeightThresholdPolicy {

    static final String WARN_KEY = "process.weightTolerancePercent";
    static final String BLOCK_KEY = "process.weightBlockTolerancePercent";
    private static final int STATUS_ENABLED = 1;
    private static final BigDecimal DEFAULT_WARN = new BigDecimal("3");
    private static final BigDecimal DEFAULT_BLOCK = new BigDecimal("5");

    private final SysConfigItemMapper configItemMapper;

    public void validateEffectivePair(String proposedKey, String proposedValue, Integer proposedStatus) {
        if (!WARN_KEY.equals(proposedKey) && !BLOCK_KEY.equals(proposedKey)) {
            return;
        }
        Map<String, SysConfigItem> current = lockThresholds();
        BigDecimal warn = effectiveValue(WARN_KEY, proposedKey, proposedValue, proposedStatus, current);
        BigDecimal block = effectiveValue(BLOCK_KEY, proposedKey, proposedValue, proposedStatus, current);
        if (warn != null && block != null && block.compareTo(warn) < 0) {
            throw new BusinessException("重量拦截阈值不能小于警告阈值");
        }
    }

    private Map<String, SysConfigItem> lockThresholds() {
        List<SysConfigItem> items = configItemMapper.selectList(new LambdaQueryWrapper<SysConfigItem>()
                .in(SysConfigItem::getConfigKey, List.of(WARN_KEY, BLOCK_KEY))
                .orderByAsc(SysConfigItem::getConfigKey)
                .last("FOR UPDATE"));
        return items.stream().collect(Collectors.toMap(
                SysConfigItem::getConfigKey, Function.identity(), (left, right) -> left));
    }

    private BigDecimal effectiveValue(String key, String proposedKey, String proposedValue, Integer proposedStatus,
                                      Map<String, SysConfigItem> current) {
        if (key.equals(proposedKey)) {
            return Integer.valueOf(STATUS_ENABLED).equals(proposedStatus)
                    ? new BigDecimal(proposedValue.trim()) : defaultValue(key);
        }
        SysConfigItem item = current.get(key);
        if (item == null || !Integer.valueOf(STATUS_ENABLED).equals(item.getStatus())
                || !StringUtils.hasText(item.getConfigValue())) {
            return defaultValue(key);
        }
        try {
            return new BigDecimal(item.getConfigValue().trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException("重量阈值历史配置格式不正确: " + key);
        }
    }

    private BigDecimal defaultValue(String key) {
        return WARN_KEY.equals(key) ? DEFAULT_WARN : DEFAULT_BLOCK;
    }
}
