package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliverySettlementBlockPolicyTest {

    @Test
    void resolveAction_whenNoRisk_returnsNone() {
        DeliverySettlementBlockPolicy policy = policy(List.of());

        int action = policy.resolveAction(false, false, "出库");

        assertEquals(DeliverySettlementBlockPolicy.ACTION_NONE, action);
    }

    @Test
    void resolveAction_whenDefaultModeAndForceRelease_returnsRelease() {
        DeliverySettlementBlockPolicy policy = policy(List.of());

        int action = policy.resolveAction(true, true, "出库");

        assertEquals(DeliverySettlementBlockPolicy.ACTION_RELEASE, action);
    }

    @Test
    void resolveAction_whenDefaultModeWithoutForceRelease_returnsRelease() {
        DeliverySettlementBlockPolicy policy = policy(List.of());

        int action = policy.resolveAction(true, false, "出库");

        assertEquals(DeliverySettlementBlockPolicy.ACTION_RELEASE, action);
    }

    @Test
    void resolveAction_whenRejectMode_blocksEvenWithForceRelease() {
        DeliverySettlementBlockPolicy policy = policy(List.of(config("2")));

        assertThrows(BusinessException.class, () -> policy.resolveAction(true, true, "出库"));
    }

    @Test
    void resolveAction_whenOffMode_returnsNone() {
        DeliverySettlementBlockPolicy policy = policy(List.of(config("0")));

        int action = policy.resolveAction(true, false, "出库");

        assertEquals(DeliverySettlementBlockPolicy.ACTION_NONE, action);
    }

    private DeliverySettlementBlockPolicy policy(List<SysConfigItem> configs) {
        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.enabledByKeys(List.of(DeliverySettlementBlockPolicy.CONFIG_KEY))).thenReturn(configs);
        return new DeliverySettlementBlockPolicy(configService);
    }

    private SysConfigItem config(String value) {
        SysConfigItem item = new SysConfigItem();
        item.setConfigKey(DeliverySettlementBlockPolicy.CONFIG_KEY);
        item.setConfigValue(value);
        return item;
    }
}
