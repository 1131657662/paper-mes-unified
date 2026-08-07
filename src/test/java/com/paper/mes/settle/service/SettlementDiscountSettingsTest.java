package com.paper.mes.settle.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettlementDiscountSettingsTest {

    private SystemConfigService configService;
    private SettlementDiscountSettings settings;

    @BeforeEach
    void setUp() {
        configService = mock(SystemConfigService.class);
        when(configService.enabledByKeys(any())).thenReturn(List.of(
                config("settle.discountAutoApproveLimit", "1.00"),
                config("settle.discountMaxAmount", "500.00"),
                config("settle.discountMaxPercent", "10.00")));
        settings = new SettlementDiscountSettings(configService);
    }

    @Test
    void fullWaiverWithIndependentApprovalCanUseEntireOutstandingAmount() {
        assertDoesNotThrow(() -> settings.requireAllowed(new BigDecimal("150.00"), new BigDecimal("150.00")));
        assertTrue(settings.requiresApproval(new BigDecimal("150.00"), new BigDecimal("150.00")));
    }

    @Test
    void partialDiscountAboveThresholdIsAllowedAfterApproval() {
        assertDoesNotThrow(() -> settings.requireAllowed(new BigDecimal("1000.00"), new BigDecimal("1500.00")));
        assertTrue(settings.requiresApproval(new BigDecimal("1000.00"), new BigDecimal("1500.00")));
    }

    @Test
    void fullWaiverAboveThresholdIsAllowedAfterApproval() {
        assertDoesNotThrow(() -> settings.requireAllowed(new BigDecimal("600.00"), new BigDecimal("600.00")));
        assertTrue(settings.requiresApproval(new BigDecimal("600.00"), new BigDecimal("600.00")));
    }

    @Test
    void discountAboveOutstandingAmountIsRejected() {
        assertThrows(BusinessException.class,
                () -> settings.requireAllowed(new BigDecimal("1500.01"), new BigDecimal("1500.00")));
    }

    @Test
    void classifiesDirectFinanceAndAdminApprovalLevels() {
        assertEquals(SettlementDiscountApprovalLevel.DIRECT,
                settings.approvalLevel(new BigDecimal("1.00"), new BigDecimal("1500.00")));
        assertEquals(SettlementDiscountApprovalLevel.FINANCE,
                settings.approvalLevel(new BigDecimal("5.00"), new BigDecimal("1500.00")));
        assertEquals(SettlementDiscountApprovalLevel.ADMIN,
                settings.approvalLevel(new BigDecimal("1000.00"), new BigDecimal("1500.00")));
        assertEquals(SettlementDiscountApprovalLevel.ADMIN,
                settings.approvalLevel(new BigDecimal("150.00"), new BigDecimal("150.00")));
    }

    private static SysConfigItem config(String key, String value) {
        SysConfigItem item = new SysConfigItem();
        item.setConfigKey(key);
        item.setConfigValue(value);
        return item;
    }
}
