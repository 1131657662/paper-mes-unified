package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliverySettlementBlockPolicyTest {

    @Test
    void resolveAction_whenNoRisk_returnsNone() {
        PermissionChecker checker = mock(PermissionChecker.class);
        DeliverySettlementBlockPolicy policy = policy(List.of(), checker);

        int action = policy.resolveAction(false, false, "出库");

        assertEquals(DeliverySettlementBlockPolicy.ACTION_NONE, action);
    }

    @Test
    void resolveReleaseAction_whenNoRisk_requiresDeliveryManage() {
        PermissionChecker checker = mock(PermissionChecker.class);
        DeliverySettlementBlockPolicy policy = policy(List.of(), checker);

        int action = policy.resolveReleaseAction(false, false, "确认出库");

        assertEquals(DeliverySettlementBlockPolicy.ACTION_NONE, action);
        verify(checker).require(Permissions.DELIVERY_MANAGE);
    }

    @Test
    void resolveAction_whenDefaultModeAndForceRelease_returnsRelease() {
        PermissionChecker checker = mock(PermissionChecker.class);
        DeliverySettlementBlockPolicy policy = policy(List.of(), checker);

        int action = policy.resolveReleaseAction(true, true, "出库");

        assertEquals(DeliverySettlementBlockPolicy.ACTION_RELEASE, action);
        verify(checker).require(Permissions.DELIVERY_RELEASE);
    }

    @Test
    void resolveAction_whenDefaultModeWithoutForceRelease_requiresAuthorization() {
        DeliverySettlementBlockPolicy policy = policy(List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.resolveAction(true, false, "出库"));

        assertEquals(ErrorCode.E010.getCode(), error.getErrorCode());
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

    @Test
    void resolveAction_whenForceReleaseWithoutPermission_returnsForbidden() {
        PermissionChecker checker = mock(PermissionChecker.class);
        doThrow(new BusinessException(403, "当前账号没有放行权限"))
                .when(checker).require(Permissions.DELIVERY_RELEASE);
        DeliverySettlementBlockPolicy policy = policy(List.of(), checker);

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.resolveReleaseAction(true, true, "出库"));

        assertEquals(403, error.getCode());
    }

    private DeliverySettlementBlockPolicy policy(List<SysConfigItem> configs) {
        return policy(configs, mock(PermissionChecker.class));
    }

    private DeliverySettlementBlockPolicy policy(List<SysConfigItem> configs, PermissionChecker permissionChecker) {
        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.enabledByKeys(List.of(DeliverySettlementBlockPolicy.CONFIG_KEY))).thenReturn(configs);
        return new DeliverySettlementBlockPolicy(configService, permissionChecker);
    }

    private SysConfigItem config(String value) {
        SysConfigItem item = new SysConfigItem();
        item.setConfigKey(DeliverySettlementBlockPolicy.CONFIG_KEY);
        item.setConfigValue(value);
        return item;
    }
}
