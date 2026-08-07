package com.paper.mes.settle.service;

import com.paper.mes.common.BusinessException;
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
        BigDecimal normalizedDiscount = money(discount);
        BigDecimal normalizedUnreceived = money(unreceived);
        if (normalizedDiscount.signum() < 0) {
            throw new BusinessException("优惠金额不能为负数");
        }
        if (normalizedUnreceived.signum() < 0 || normalizedDiscount.compareTo(normalizedUnreceived) > 0) {
            throw new BusinessException("优惠金额不能超过当前未收金额");
        }
    }

    public boolean requiresApproval(BigDecimal discount, BigDecimal unreceived) {
        return approvalLevel(discount, unreceived) != SettlementDiscountApprovalLevel.DIRECT;
    }

    public SettlementDiscountApprovalLevel approvalLevel(BigDecimal discount, BigDecimal unreceived) {
        BigDecimal normalizedDiscount = money(discount);
        if (normalizedDiscount.signum() <= 0) return SettlementDiscountApprovalLevel.DIRECT;
        Settings limits = current();
        BigDecimal normalizedUnreceived = money(unreceived);
        if (normalizedDiscount.compareTo(limits.autoApproveLimit()) <= 0) {
            return SettlementDiscountApprovalLevel.DIRECT;
        }
        boolean withinFinanceAmount = normalizedDiscount.compareTo(limits.maxAmount()) <= 0;
        boolean withinFinancePercent = normalizedUnreceived.signum() > 0
                && discountPercent(normalizedDiscount, normalizedUnreceived)
                .compareTo(limits.maxPercent()) <= 0;
        return withinFinanceAmount && withinFinancePercent
                ? SettlementDiscountApprovalLevel.FINANCE
                : SettlementDiscountApprovalLevel.ADMIN;
    }

    public BigDecimal discountPercent(BigDecimal discount, BigDecimal unreceived) {
        BigDecimal normalizedUnreceived = money(unreceived);
        if (normalizedUnreceived.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        return money(discount).multiply(BigDecimal.valueOf(100))
                .divide(normalizedUnreceived, 2, RoundingMode.HALF_UP);
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
