package com.paper.mes.processorder.service;

import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessStepPricingSettingsTest {

    @Test
    void autoApproveLimit_readsConfiguredAmount() {
        SystemConfigService config = mock(SystemConfigService.class);
        SysConfigItem item = new SysConfigItem();
        item.setConfigValue("250.00");
        when(config.enabledByKeys(List.of(ProcessStepPricingSettings.AUTO_APPROVE_LIMIT_KEY)))
                .thenReturn(List.of(item));

        BigDecimal limit = new ProcessStepPricingSettings(config).autoApproveLimit();

        assertThat(limit).isEqualByComparingTo("250.00");
    }

    @Test
    void autoApproveLimit_whenConfiguredValueIsInvalid_usesSafeDefault() {
        SystemConfigService config = mock(SystemConfigService.class);
        SysConfigItem item = new SysConfigItem();
        item.setConfigValue("-1");
        when(config.enabledByKeys(List.of(ProcessStepPricingSettings.AUTO_APPROVE_LIMIT_KEY)))
                .thenReturn(List.of(item));

        BigDecimal limit = new ProcessStepPricingSettings(config).autoApproveLimit();

        assertThat(limit).isEqualByComparingTo("100.00");
    }
}
