package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliverySettlementBlockPolicy {

    public static final String CONFIG_KEY = "delivery.cashSettleBlockMode";
    public static final int ACTION_NONE = 0;
    public static final int ACTION_RELEASE = 1;
    public static final int MODE_OFF = 0;
    public static final int MODE_RELEASE = 1;
    public static final int MODE_REJECT = 2;

    private final SystemConfigService systemConfigService;

    public int resolveAction(boolean hasUnsettledCashOrders, boolean forceRelease, String operationName) {
        if (!hasUnsettledCashOrders) {
            return ACTION_NONE;
        }
        int mode = currentMode();
        if (mode == MODE_OFF) {
            return ACTION_NONE;
        }
        if (mode == MODE_REJECT) {
            throw new BusinessException("次结加工单存在未结清款项，系统已启用强制拦截，禁止" + operationName);
        }
        return ACTION_RELEASE;
    }

    private int currentMode() {
        return systemConfigService.enabledByKeys(List.of(CONFIG_KEY)).stream()
                .findFirst()
                .map(SysConfigItem::getConfigValue)
                .map(this::parseMode)
                .orElse(MODE_RELEASE);
    }

    private int parseMode(String value) {
        try {
            int mode = Integer.parseInt(value == null ? "" : value.trim());
            return mode == MODE_OFF || mode == MODE_REJECT ? mode : MODE_RELEASE;
        } catch (NumberFormatException ignored) {
            return MODE_RELEASE;
        }
    }
}
