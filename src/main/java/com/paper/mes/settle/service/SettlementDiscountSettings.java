package com.paper.mes.settle.service;

import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementDiscountSettings {
    public static final String AUTO_LIMIT_KEY = "settle.discountAutoApproveLimit";
    public static final String MAX_AMOUNT_KEY = "settle.discountMaxAmount";
    public static final String MAX_PERCENT_KEY = "settle.discountMaxPercent";

    private static final BigDecimal DEFAULT_AUTO_LIMIT = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_MAX_AMOUNT = new BigDecimal("500.00");
    private static final BigDecimal DEFAULT_MAX_PERCENT = new BigDecimal("10.00");

    private final SystemConfigService systemConfigService;

    public Settings current() {
        Map<String, String> values = systemConfigService.enabledByKeys(List.of(
                        AUTO_LIMIT_KEY, MAX_AMOUNT_KEY, MAX_PERCENT_KEY)).stream()
                .collect(Collectors.toMap(SysConfigItem::getConfigKey, SysConfigItem::getConfigValue));
        return new Settings(read(values, AUTO_LIMIT_KEY, DEFAULT_AUTO_LIMIT),
                read(values, MAX_AMOUNT_KEY, DEFAULT_MAX_AMOUNT),
                read(values, MAX_PERCENT_KEY, DEFAULT_MAX_PERCENT));
    }

    public void requireAllowed(BigDecimal discount, BigDecimal unreceived) {
        Settings limits = current();
        BigDecimal ratioCap = money(unreceived).multiply(limits.maxPercent())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal cap = limits.maxAmount().min(ratioCap);
        if (money(discount).compareTo(cap) > 0) {
            throw new com.paper.mes.common.BusinessException("优惠金额超过系统允许上限 " + cap);
        }
    }

    public boolean requiresApproval(BigDecimal discount) {
        return money(discount).compareTo(current().autoApproveLimit()) > 0;
    }

    private BigDecimal read(Map<String, String> values, String key, BigDecimal fallback) {
        try {
            BigDecimal value = new BigDecimal(values.getOrDefault(key, fallback.toPlainString()));
            return value.signum() >= 0 ? value : fallback;
        } catch (NumberFormatException exception) {
            log.warn("Invalid settlement discount setting {}, using {}", key, fallback);
            return fallback;
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public record Settings(BigDecimal autoApproveLimit, BigDecimal maxAmount, BigDecimal maxPercent) {
    }
}
