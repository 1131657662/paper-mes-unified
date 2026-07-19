package com.paper.mes.processorder.service;

import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Thresholds for dual-control pricing adjustments. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessStepPricingSettings {

    public static final String AUTO_APPROVE_LIMIT_KEY = "process.pricingAutoApproveLimit";
    private static final BigDecimal DEFAULT_AUTO_APPROVE_LIMIT = new BigDecimal("100.00");

    private final SystemConfigService systemConfigService;

    public BigDecimal autoApproveLimit() {
        List<SysConfigItem> items = systemConfigService.enabledByKeys(List.of(AUTO_APPROVE_LIMIT_KEY));
        if (items.isEmpty() || items.getFirst().getConfigValue() == null) {
            return DEFAULT_AUTO_APPROVE_LIMIT;
        }
        try {
            BigDecimal value = new BigDecimal(items.getFirst().getConfigValue());
            return value.signum() >= 0 ? value.setScale(2, java.math.RoundingMode.HALF_UP)
                    : DEFAULT_AUTO_APPROVE_LIMIT;
        } catch (NumberFormatException exception) {
            log.warn("Invalid process pricing approval limit, using {}", DEFAULT_AUTO_APPROVE_LIMIT);
            return DEFAULT_AUTO_APPROVE_LIMIT;
        }
    }
}
