package com.paper.mes.system.config.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.dto.ConfigItemSaveDTO;
import com.paper.mes.system.config.entity.SysConfigItem;

import java.util.Objects;

final class BuiltInConfigMetadataGuard {

    private BuiltInConfigMetadataGuard() {
    }

    static void ensureUnchanged(SysConfigItem item, ConfigItemSaveDTO dto) {
        boolean changed = !Objects.equals(item.getConfigGroup(), dto.getConfigGroup())
                || !Objects.equals(item.getConfigKey(), dto.getConfigKey())
                || !Objects.equals(item.getConfigName(), dto.getConfigName())
                || !Objects.equals(item.getValueType(), dto.getValueType())
                || !Objects.equals(item.getUnit(), dto.getUnit())
                || !Objects.equals(item.getSortNo(), dto.getSortNo())
                || !Objects.equals(item.getRemark(), dto.getRemark());
        if (changed) {
            throw new BusinessException("内置系统参数只允许修改参数值和启停状态");
        }
    }
}
